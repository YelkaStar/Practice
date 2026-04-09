import java.io.*;
import java.util.*;

/**
 * Головний клас для демонстрації серіалізації та десеріалізації об'єктів,
 * обчислення дальності польоту по балістичній траєкторії, зберігання
 * результатів у колекції, шаблону Factory Method, а також механізмів
 * заміщення (overriding), перевантаження (overloading) та динамічного
 * призначення методів (polymorphism).
 *
 * <p>Формула: {@code s = v0² · sin(2α) / g}</p>
 *
 * <p>Ієрархія відображення:</p>
 * <pre>
 *  Displayable (interface)
 *    └── ConfigurableDisplay (interface, extends Displayable)
 *  TextDisplay implements Displayable
 *    └── SimpleTableDisplay extends TextDisplay, implements ConfigurableDisplay
 *          └── BorderedTableDisplay extends SimpleTableDisplay
 *  DetailedTextDisplay implements Displayable
 *  CompactDisplay implements Displayable
 *  CsvTableDisplay implements ConfigurableDisplay
 * </pre>
 *
 * <p>Ієрархія фабрик:</p>
 * <pre>
 *  DisplayFactory (interface) — Creator
 *    ├── TextDisplayFactory
 *    ├── DetailedDisplayFactory
 *    ├── CompactDisplayFactory
 *    ├── SimpleTableDisplayFactory
 *    ├── BorderedTableDisplayFactory
 *    └── CsvTableDisplayFactory
 * </pre>
 *
 * @author Eduard
 * @version 3.0
 */
public class BallisticApp {

    // =========================================================================
    // BallisticData
    // =========================================================================

    /**
     * Клас для зберігання параметрів та результатів обчислення
     * дальності польоту по балістичній траєкторії.
     *
     * <p>Реалізує {@link Serializable}. Поля {@code timestamp} та
     * {@code statusMessage} — {@code transient}, не зберігаються.</p>
     *
     * @see BallisticSolver
     */
    static class BallisticData implements Serializable {

        /** Ідентифікатор версії серіалізації. */
        private static final long serialVersionUID = 1L;

        /** Прискорення вільного падіння (м/с²). */
        public static final double G_METRIC = 9.80665;

        /** Прискорення вільного падіння (фут/с²). */
        public static final double G_IMPERIAL = 32.174;

        /** Коефіцієнт переведення метрів у фути. */
        public static final double METERS_TO_FEET = 3.28084;

        /** Початкова швидкість (м/с). */
        private double initialVelocity;

        /** Кут пострілу до поверхні Землі (градуси). */
        private double angleDegrees;

        /** Дальність польоту в метричній системі (метри). */
        private double rangeMetric;

        /** Дальність польоту в англійській системі (фути). */
        private double rangeImperial;

        /**
         * Мітка часу створення об'єкта (мс). {@code transient} — не серіалізується.
         * Після десеріалізації: {@code 0L}.
         */
        private transient long timestamp;

        /**
         * Допоміжне повідомлення про останню операцію. {@code transient} — не серіалізується.
         * Після десеріалізації: {@code null}.
         */
        private transient String statusMessage;

        /**
         * Створює об'єкт з початковими параметрами.
         *
         * @param initialVelocity початкова швидкість (м/с), &ge; 0
         * @param angleDegrees    кут (градуси), від 0 до 90
         * @throws IllegalArgumentException якщо параметри поза допустимим діапазоном
         */
        public BallisticData(double initialVelocity, double angleDegrees) {
            if (initialVelocity < 0)
                throw new IllegalArgumentException(
                        "Початкова швидкість не може бути від'ємною: " + initialVelocity);
            if (angleDegrees < 0 || angleDegrees > 90)
                throw new IllegalArgumentException(
                        "Кут має бути в діапазоні [0; 90]: " + angleDegrees);
            this.initialVelocity = initialVelocity;
            this.angleDegrees    = angleDegrees;
            this.rangeMetric     = 0.0;
            this.rangeImperial   = 0.0;
            this.timestamp       = System.currentTimeMillis();
            this.statusMessage   = "Об'єкт створено, обчислення ще не виконано.";
        }

        /** @return початкова швидкість (м/с) */
        public double getInitialVelocity() { return initialVelocity; }

        /** @param v швидкість (м/с), &ge; 0 */
        public void setInitialVelocity(double v) {
            if (v < 0) throw new IllegalArgumentException("Швидкість < 0");
            this.initialVelocity = v;
        }

        /** @return кут (градуси) */
        public double getAngleDegrees() { return angleDegrees; }

        /** @param a кут (градуси), від 0 до 90 */
        public void setAngleDegrees(double a) {
            if (a < 0 || a > 90) throw new IllegalArgumentException("Кут поза [0;90]");
            this.angleDegrees = a;
        }

        /** @return дальність у метрах */
        public double getRangeMetric() { return rangeMetric; }

        /** @param r дальність (м) */
        public void setRangeMetric(double r) { this.rangeMetric = r; }

        /** @return дальність у футах */
        public double getRangeImperial() { return rangeImperial; }

        /** @param r дальність (фут) */
        public void setRangeImperial(double r) { this.rangeImperial = r; }

        /** @return мітка часу (мс) або {@code 0} після десеріалізації */
        public long getTimestamp() { return timestamp; }

        /** @param t мітка часу (мс) */
        public void setTimestamp(long t) { this.timestamp = t; }

        /** @return повідомлення або {@code null} після десеріалізації */
        public String getStatusMessage() { return statusMessage; }

        /** @param s текст повідомлення */
        public void setStatusMessage(String s) { this.statusMessage = s; }

        /**
         * Повертає текстове представлення об'єкта з усіма полями.
         *
         * @return рядок із параметрами та результатами
         */
        @Override
        public String toString() {
            return String.format(
                "BallisticData {%n" +
                "  Початкова швидкість : %.2f м/с%n" +
                "  Кут                 : %.2f°%n" +
                "  Дальність (метрич.) : %.4f м%n" +
                "  Дальність (англ.)   : %.4f фут%n" +
                "  Timestamp (transient): %d%n" +
                "  Status    (transient): %s%n" +
                "}",
                initialVelocity, angleDegrees,
                rangeMetric, rangeImperial,
                timestamp, statusMessage);
        }
    }

    // =========================================================================
    // BallisticSolver
    // =========================================================================

    /**
     * Клас для обчислення дальності польоту по балістичній траєкторії.
     *
     * <p>Використовує агрегування: містить посилання на {@link BallisticData}.</p>
     * <p>Формула: {@code s = v0² · sin(2α) / g}</p>
     *
     * @see BallisticData
     */
    static class BallisticSolver {

        /** Агрегований об'єкт даних. */
        private BallisticData data;

        /**
         * Створює розв'язувач із заданим об'єктом даних.
         *
         * @param data об'єкт параметрів; не {@code null}
         * @throws IllegalArgumentException якщо {@code data == null}
         */
        public BallisticSolver(BallisticData data) {
            if (data == null)
                throw new IllegalArgumentException("Об'єкт даних не може бути null.");
            this.data = data;
        }

        /**
         * Створює розв'язувач, автоматично формуючи об'єкт даних.
         *
         * @param velocity початкова швидкість (м/с)
         * @param angle    кут пострілу (градуси)
         */
        public BallisticSolver(double velocity, double angle) {
            this.data = new BallisticData(velocity, angle);
        }

        /** @return поточний об'єкт {@link BallisticData} */
        public BallisticData getData() { return data; }

        /** @param data новий об'єкт {@link BallisticData} */
        public void setData(BallisticData data) {
            if (data == null) throw new IllegalArgumentException("data == null");
            this.data = data;
        }

        /**
         * Обчислює дальність у метричній системі (метри).
         *
         * @return дальність (м)
         */
        public double computeRangeMetric() {
            double v0       = data.getInitialVelocity();
            double angleRad = Math.toRadians(data.getAngleDegrees());
            return (v0 * v0 * Math.sin(2.0 * angleRad)) / BallisticData.G_METRIC;
        }

        /**
         * Обчислює дальність в англійській системі (фути).
         *
         * @return дальність (фут)
         */
        public double computeRangeImperial() {
            double v0Feet   = data.getInitialVelocity() * BallisticData.METERS_TO_FEET;
            double angleRad = Math.toRadians(data.getAngleDegrees());
            return (v0Feet * v0Feet * Math.sin(2.0 * angleRad)) / BallisticData.G_IMPERIAL;
        }

        /**
         * Виконує повне обчислення та записує результати в агрегований об'єкт.
         */
        public void solve() {
            data.setRangeMetric(computeRangeMetric());
            data.setRangeImperial(computeRangeImperial());
            data.setStatusMessage("Обчислення виконано успішно.");
        }

        /**
         * Повертає форматований звіт із результатами обчислень.
         *
         * @return багаторядковий текстовий звіт
         */
        public String getReport() {
            return String.format(
                "========== Балістичний розрахунок ==========%n" +
                "  v0    = %.2f м/с%n" +
                "  α     = %.2f°%n" +
                "---------- Результати ---------------------%n" +
                "  Метрична система  : %.4f м (%.4f км)%n" +
                "  Англійська система: %.4f фут (%.4f миль)%n" +
                "============================================",
                data.getInitialVelocity(), data.getAngleDegrees(),
                data.getRangeMetric(), data.getRangeMetric() / 1000.0,
                data.getRangeImperial(), data.getRangeImperial() / 5280.0);
        }
    }

    // =========================================================================
    // ResultCollection
    // =========================================================================

    /**
     * Колекція результатів балістичних обчислень з підтримкою серіалізації.
     *
     * <p>Зберігає об'єкти {@link BallisticData} в {@link ArrayList}.
     * Надає методи додавання, очищення, отримання, збереження та відновлення.</p>
     */
    static class ResultCollection implements Serializable {

        /** Ідентифікатор версії серіалізації. */
        private static final long serialVersionUID = 2L;

        /** Внутрішній список результатів. */
        private final List<BallisticData> results = new ArrayList<>();

        /**
         * Додає результат до колекції.
         *
         * @param data об'єкт {@link BallisticData}; не {@code null}
         * @throws IllegalArgumentException якщо {@code data == null}
         */
        public void add(BallisticData data) {
            if (data == null) throw new IllegalArgumentException("data == null");
            results.add(data);
        }

        /**
         * Повертає незмінний список усіх збережених результатів.
         *
         * @return список {@link BallisticData}
         */
        public List<BallisticData> getAll() {
            return Collections.unmodifiableList(results);
        }

        /**
         * Повертає кількість елементів у колекції.
         *
         * @return розмір колекції
         */
        public int size() { return results.size(); }

        /** Очищає колекцію. */
        public void clear() { results.clear(); }

        /**
         * Серіалізує колекцію у файл.
         *
         * @param filename ім'я файлу
         * @throws IOException якщо виникла помилка запису
         */
        public void saveToFile(String filename) throws IOException {
            try (ObjectOutputStream oos =
                     new ObjectOutputStream(new FileOutputStream(filename))) {
                oos.writeObject(this);
            }
        }

        /**
         * Відновлює колекцію з файлу та замінює поточний вміст.
         *
         * @param filename ім'я файлу
         * @throws IOException            помилка читання
         * @throws ClassNotFoundException клас не знайдено
         */
        public void loadFromFile(String filename)
                throws IOException, ClassNotFoundException {
            try (ObjectInputStream ois =
                     new ObjectInputStream(new FileInputStream(filename))) {
                ResultCollection loaded = (ResultCollection) ois.readObject();
                results.clear();
                results.addAll(loaded.results);
            }
        }

        /**
         * Повертає текстовий підсумок колекції.
         *
         * @return рядок з кількістю записів
         */
        @Override
        public String toString() {
            return String.format("ResultCollection { розмір=%d }", results.size());
        }
    }

    // =========================================================================
    // TableConfig — параметри таблиці, що задаються користувачем
    // =========================================================================

    /**
     * Клас конфігурації параметрів відображення таблиці.
     *
     * <p>Дозволяє користувачу налаштовувати:</p>
     * <ul>
     *   <li>ширину стовпців;</li>
     *   <li>символ-роздільник;</li>
     *   <li>видимість стовпців метричної та англійської систем;</li>
     *   <li>максимальну кількість рядків;</li>
     *   <li>заголовок таблиці;</li>
     *   <li>режим відображення кілометрів/миль поряд з основними одиницями.</li>
     * </ul>
     *
     * @see SimpleTableDisplay
     * @see BorderedTableDisplay
     * @see CsvTableDisplay
     */
    static class TableConfig implements Serializable {

        /** Ідентифікатор версії серіалізації. */
        private static final long serialVersionUID = 3L;

        /** Ширина числового стовпця (символи). */
        private int columnWidth;

        /** Символ-роздільник між стовпцями. */
        private char separator;

        /** Показувати метричну систему (метри). */
        private boolean showMetric;

        /** Показувати англійську систему (фути). */
        private boolean showImperial;

        /** Максимальна кількість рядків (0 = всі). */
        private int maxRows;

        /** Заголовок таблиці. */
        private String title;

        /** Показувати похідні одиниці (км / милі). */
        private boolean showDerived;

        /**
         * Створює конфігурацію за замовчуванням:
         * ширина=10, роздільник='|', показувати обидві системи,
         * всі рядки, заголовок "Результати балістики", без похідних одиниць.
         */
        public TableConfig() {
            this.columnWidth  = 10;
            this.separator    = '|';
            this.showMetric   = true;
            this.showImperial = true;
            this.maxRows      = 0;
            this.title        = "Результати балістики";
            this.showDerived  = false;
        }

        /**
         * Створює конфігурацію з повним набором параметрів.
         *
         * @param columnWidth  ширина числового стовпця (&ge; 4)
         * @param separator    символ-роздільник
         * @param showMetric   показувати метричну систему
         * @param showImperial показувати англійську систему
         * @param maxRows      максимум рядків (0 = всі)
         * @param title        заголовок таблиці
         * @param showDerived  показувати км/милі
         */
        public TableConfig(int columnWidth, char separator,
                           boolean showMetric, boolean showImperial,
                           int maxRows, String title, boolean showDerived) {
            this.columnWidth  = Math.max(4, columnWidth);
            this.separator    = separator;
            this.showMetric   = showMetric;
            this.showImperial = showImperial;
            this.maxRows      = Math.max(0, maxRows);
            this.title        = (title != null && !title.isBlank()) ? title : "Таблиця";
            this.showDerived  = showDerived;
        }

        /** @return ширина стовпця */
        public int getColumnWidth() { return columnWidth; }

        /** @param w ширина (&ge; 4) */
        public void setColumnWidth(int w) { this.columnWidth = Math.max(4, w); }

        /** @return символ-роздільник */
        public char getSeparator() { return separator; }

        /** @param s символ-роздільник */
        public void setSeparator(char s) { this.separator = s; }

        /** @return чи показувати метри */
        public boolean isShowMetric() { return showMetric; }

        /** @param v показувати метри */
        public void setShowMetric(boolean v) { this.showMetric = v; }

        /** @return чи показувати фути */
        public boolean isShowImperial() { return showImperial; }

        /** @param v показувати фути */
        public void setShowImperial(boolean v) { this.showImperial = v; }

        /** @return максимум рядків (0 = всі) */
        public int getMaxRows() { return maxRows; }

        /** @param r максимум (&ge; 0) */
        public void setMaxRows(int r) { this.maxRows = Math.max(0, r); }

        /** @return заголовок */
        public String getTitle() { return title; }

        /** @param t заголовок */
        public void setTitle(String t) {
            this.title = (t != null && !t.isBlank()) ? t : "Таблиця";
        }

        /** @return чи показувати похідні одиниці */
        public boolean isShowDerived() { return showDerived; }

        /** @param v показувати km/miles */
        public void setShowDerived(boolean v) { this.showDerived = v; }

        /**
         * Обчислює ефективну кількість рядків для відображення.
         *
         * @param total загальна кількість записів
         * @return кількість рядків до відображення
         */
        public int effectiveRows(int total) {
            return (maxRows == 0 || maxRows > total) ? total : maxRows;
        }

        /**
         * Підраховує кількість активних стовпців даних (без № та v0, α).
         *
         * @return кількість стовпців з результатами
         */
        public int activeDataColumns() {
            int c = 0;
            if (showMetric)   c += showDerived ? 2 : 1;
            if (showImperial) c += showDerived ? 2 : 1;
            return c;
        }

        /**
         * Повертає текстове представлення конфігурації.
         *
         * @return рядок з описом усіх параметрів
         */
        @Override
        public String toString() {
            return String.format(
                "TableConfig{ ширина=%d, роздільник='%c', метрична=%b, " +
                "англійська=%b, макс_рядків=%s, похідні=%b, заголовок=\"%s\" }",
                columnWidth, separator, showMetric, showImperial,
                maxRows == 0 ? "всі" : String.valueOf(maxRows),
                showDerived, title);
        }
    }

    // =========================================================================
    // Displayable — інтерфейс продукту (базовий)
    // =========================================================================

    /**
     * Базовий інтерфейс для об'єктів, що відображають результати обчислень.
     *
     * <p>Роль <b>Product</b> у шаблоні Factory Method.</p>
     *
     * @see ConfigurableDisplay
     * @see DisplayFactory
     */
    interface Displayable {

        /**
         * Виводить короткий рядок із ключовими результатами.
         */
        void displayShort();

        /**
         * Виводить повний звіт по одному об'єкту {@link BallisticData}.
         */
        void displayFull();

        /**
         * Виводить всі результати колекції.
         *
         * @param results список {@link BallisticData} для відображення
         */
        void displayTable(List<BallisticData> results);
    }

    // =========================================================================
    // ConfigurableDisplay — розширений інтерфейс з перевантаженими методами
    // =========================================================================

    /**
     * Розширений інтерфейс відображення з параметрами, що задаються користувачем.
     *
     * <p>Розширює {@link Displayable}, додаючи <b>перевантажені (overloaded)</b>
     * версії {@code displayTable}:</p>
     * <ul>
     *   <li>{@link #displayTable(List, int)} — обмеження по кількості рядків;</li>
     *   <li>{@link #displayTable(List, TableConfig)} — повна конфігурація.</li>
     * </ul>
     *
     * <p>Демонстрація <b>overloading</b>: три методи з однаковою назвою
     * {@code displayTable} та різними сигнатурами.</p>
     *
     * @see SimpleTableDisplay
     * @see BorderedTableDisplay
     * @see CsvTableDisplay
     */
    interface ConfigurableDisplay extends Displayable {

        /**
         * Виводить не більше {@code maxRows} рядків таблиці.
         * <p>Перевантаження (overloading) відносно {@link #displayTable(List)}.</p>
         *
         * @param results список даних
         * @param maxRows максимальна кількість рядків (&ge; 1)
         */
        void displayTable(List<BallisticData> results, int maxRows);

        /**
         * Виводить таблицю з повною користувацькою конфігурацією.
         * <p>Перевантаження (overloading) відносно {@link #displayTable(List)}.</p>
         *
         * @param results список даних
         * @param config  об'єкт конфігурації {@link TableConfig}
         */
        void displayTable(List<BallisticData> results, TableConfig config);
    }

    // =========================================================================
    // DisplayFactory — інтерфейс фабрики (Creator)
    // =========================================================================

    /**
     * Інтерфейс фабрики для створення об'єктів {@link Displayable}.
     *
     * <p>Роль <b>Creator</b> у шаблоні Factory Method.
     * Підкласи перевизначають {@link #createDisplay(BallisticData)} без
     * зміни клієнтського коду.</p>
     *
     * @see TextDisplayFactory
     * @see SimpleTableDisplayFactory
     * @see BorderedTableDisplayFactory
     * @see CsvTableDisplayFactory
     */
    interface DisplayFactory {

        /**
         * Фабричний метод: створює об'єкт відображення.
         *
         * @param data об'єкт {@link BallisticData}
         * @return конкретний {@link Displayable}
         */
        Displayable createDisplay(BallisticData data);

        /**
         * Шаблонний метод: виконує повний цикл відображення.
         *
         * @param data    об'єкт даних
         * @param results усі збережені результати для таблиці
         */
        default void displayAll(BallisticData data, List<BallisticData> results) {
            Displayable d = createDisplay(data);
            d.displayShort();
            d.displayFull();
            d.displayTable(results);
        }
    }

    // =========================================================================
    // TextDisplay — базовий конкретний продукт
    // =========================================================================

    /**
     * Текстове відображення результатів балістичних обчислень.
     *
     * <p>Базовий конкретний <b>Product</b> у шаблоні Factory Method.
     * Служить суперкласом для {@link SimpleTableDisplay}.</p>
     *
     * @see TextDisplayFactory
     */
    static class TextDisplay implements Displayable {

        /** Дані для відображення. */
        protected final BallisticData data;

        /**
         * Створює об'єкт відображення для заданих даних.
         *
         * @param data об'єкт {@link BallisticData}
         */
        public TextDisplay(BallisticData data) {
            this.data = data;
        }

        /**
         * Виводить короткий рядок: швидкість, кут, метрична дальність.
         */
        @Override
        public void displayShort() {
            System.out.printf("[Текст] v0=%.1f м/с | α=%.1f° | дальність=%.2f м%n",
                data.getInitialVelocity(), data.getAngleDegrees(), data.getRangeMetric());
        }

        /**
         * Виводить повний звіт в обох системах мір.
         */
        @Override
        public void displayFull() {
            System.out.println("------ Текстовий звіт (TextDisplay) ------");
            System.out.printf("  Початкова швидкість : %.2f м/с%n",  data.getInitialVelocity());
            System.out.printf("  Кут пострілу        : %.2f°%n",     data.getAngleDegrees());
            System.out.printf("  Дальність (м)       : %.4f м%n",    data.getRangeMetric());
            System.out.printf("  Дальність (км)      : %.6f км%n",   data.getRangeMetric() / 1000.0);
            System.out.printf("  Дальність (фут)     : %.4f фут%n",  data.getRangeImperial());
            System.out.printf("  Дальність (миль)    : %.6f миль%n", data.getRangeImperial() / 5280.0);
            System.out.println("------------------------------------------");
        }

        /**
         * Виводить усі результати нумерованим списком.
         * <p>Метод перевизначається (overriding) у підкласах.</p>
         *
         * @param results список результатів
         */
        @Override
        public void displayTable(List<BallisticData> results) {
            System.out.println("====== Список результатів (TextDisplay) ======");
            if (results.isEmpty()) {
                System.out.println("  [порожньо]");
            } else {
                for (int i = 0; i < results.size(); i++) {
                    BallisticData r = results.get(i);
                    System.out.printf("  %2d. v0=%.1f м/с, α=%.1f° → %.2f м / %.2f фут%n",
                        i + 1, r.getInitialVelocity(), r.getAngleDegrees(),
                        r.getRangeMetric(), r.getRangeImperial());
                }
            }
            System.out.println("=============================================");
        }
    }

    // =========================================================================
    // SimpleTableDisplay — перший рівень перевизначення + перевантаження
    // =========================================================================

    /**
     * Табличне відображення з налаштованими параметрами.
     *
     * <p><b>Розширює {@link TextDisplay}</b>, демонструючи:</p>
     * <ul>
     *   <li><b>Overriding</b> — перевизначає {@link TextDisplay#displayShort()},
     *       {@link TextDisplay#displayFull()} та
     *       {@link TextDisplay#displayTable(List)};</li>
     *   <li><b>Overloading</b> — додає {@link #displayTable(List, int)} та
     *       {@link #displayTable(List, TableConfig)} з різними сигнатурами.</li>
     * </ul>
     *
     * <p>Реалізує {@link ConfigurableDisplay}, надаючи всі перевантажені версії.</p>
     *
     * @see SimpleTableDisplayFactory
     * @see BorderedTableDisplay
     */
    static class SimpleTableDisplay extends TextDisplay implements ConfigurableDisplay {

        /** Поточна конфігурація таблиці. */
        protected TableConfig config;

        /**
         * Створює об'єкт з конфігурацією за замовчуванням.
         *
         * @param data дані для відображення
         */
        public SimpleTableDisplay(BallisticData data) {
            super(data);
            this.config = new TableConfig();
        }

        /**
         * Створює об'єкт із заданою конфігурацією.
         *
         * @param data   дані для відображення
         * @param config параметри таблиці
         */
        public SimpleTableDisplay(BallisticData data, TableConfig config) {
            super(data);
            this.config = (config != null) ? config : new TableConfig();
        }

        /** @return поточна конфігурація таблиці */
        public TableConfig getConfig() { return config; }

        /** @param config нова конфігурація */
        public void setConfig(TableConfig config) {
            this.config = (config != null) ? config : new TableConfig();
        }

        /**
         * Виводить короткий рядок з назвою класу.
         * <p><b>Overriding</b> методу {@link TextDisplay#displayShort()}.</p>
         */
        @Override
        public void displayShort() {
            System.out.printf("[SimpleTable] v0=%.1f м/с | α=%.1f° | " +
                              "метрич.=%.2f м | англ.=%.2f фут%n",
                data.getInitialVelocity(), data.getAngleDegrees(),
                data.getRangeMetric(), data.getRangeImperial());
        }

        /**
         * Виводить повний звіт з міткою конфігурації.
         * <p><b>Overriding</b> методу {@link TextDisplay#displayFull()}.</p>
         */
        @Override
        public void displayFull() {
            System.out.println("------ Звіт (SimpleTableDisplay) ------");
            System.out.printf("  Конфігурація  : %s%n", config);
            System.out.printf("  v0            : %.4f м/с%n", data.getInitialVelocity());
            System.out.printf("  α             : %.4f°%n",    data.getAngleDegrees());
            if (config.isShowMetric()) {
                System.out.printf("  Метрична      : %.4f м%n", data.getRangeMetric());
                if (config.isShowDerived())
                    System.out.printf("                  %.6f км%n", data.getRangeMetric() / 1000.0);
            }
            if (config.isShowImperial()) {
                System.out.printf("  Англійська    : %.4f фут%n", data.getRangeImperial());
                if (config.isShowDerived())
                    System.out.printf("                  %.6f миль%n", data.getRangeImperial() / 5280.0);
            }
            System.out.println("---------------------------------------");
        }

        /**
         * Виводить таблицю згідно з поточною конфігурацією {@link #config}.
         * <p><b>Overriding</b> методу {@link TextDisplay#displayTable(List)}.</p>
         *
         * @param results список результатів
         */
        @Override
        public void displayTable(List<BallisticData> results) {
            displayTable(results, config);
        }

        /**
         * Виводить не більше {@code maxRows} рядків таблиці.
         * <p><b>Overloading</b>: додаткова сигнатура з {@code int maxRows}.</p>
         *
         * @param results список результатів
         * @param maxRows максимум рядків
         */
        @Override
        public void displayTable(List<BallisticData> results, int maxRows) {
            TableConfig tmp = new TableConfig(
                config.getColumnWidth(), config.getSeparator(),
                config.isShowMetric(), config.isShowImperial(),
                maxRows, config.getTitle() + " [ліміт=" + maxRows + "]",
                config.isShowDerived());
            displayTable(results, tmp);
        }

        /**
         * Виводить таблицю з повною користувацькою конфігурацією.
         * <p><b>Overloading</b>: додаткова сигнатура з {@link TableConfig}.</p>
         *
         * @param results список результатів
         * @param cfg     конфігурація таблиці
         */
        @Override
        public void displayTable(List<BallisticData> results, TableConfig cfg) {
            int w      = cfg.getColumnWidth();
            char sep   = cfg.getSeparator();
            int rows   = cfg.effectiveRows(results.size());
            String fmt = "%-" + w + "." + w + "s";

            // Формуємо заголовки стовпців
            List<String> headers = buildHeaders(cfg);
            int totalCols = 2 + headers.size(); // № + v0/α + дані
            int lineLen   = 4 + (w + 3) * totalCols;

            String hLine = String.valueOf(sep).repeat(lineLen);

            System.out.println(hLine);
            System.out.printf("%s %-" + (lineLen - 2) + "s %s%n",
                sep, cfg.getTitle(), sep);
            System.out.println(hLine);

            // Рядок заголовків
            System.out.printf("%s %-3s %s", sep, "№", sep);
            System.out.printf(" " + fmt + " %s", "v0(м/с)", sep);
            System.out.printf(" " + fmt + " %s", "α(°)", sep);
            for (String h : headers)
                System.out.printf(" " + fmt + " %s", h, sep);
            System.out.println();
            System.out.println(hLine);

            // Рядки даних
            if (results.isEmpty()) {
                System.out.printf("%s %-" + (lineLen - 2) + "s %s%n",
                    sep, "  [порожньо]", sep);
            } else {
                for (int i = 0; i < rows; i++) {
                    BallisticData r = results.get(i);
                    System.out.printf("%s %-3d %s", sep, i + 1, sep);
                    System.out.printf(" %-" + w + ".2f %s", r.getInitialVelocity(), sep);
                    System.out.printf(" %-" + w + ".2f %s", r.getAngleDegrees(), sep);
                    printDataCols(r, cfg, w, sep);
                    System.out.println();
                }
                if (rows < results.size())
                    System.out.printf("%s ... ще %d запис(ів) не показано %s%n",
                        sep, results.size() - rows, sep);
            }
            System.out.println(hLine);
        }

        /**
         * Будує список рядків заголовків стовпців за конфігурацією.
         *
         * @param cfg конфігурація
         * @return список заголовків
         */
        protected List<String> buildHeaders(TableConfig cfg) {
            List<String> h = new ArrayList<>();
            if (cfg.isShowMetric()) {
                h.add("м");
                if (cfg.isShowDerived()) h.add("км");
            }
            if (cfg.isShowImperial()) {
                h.add("фут");
                if (cfg.isShowDerived()) h.add("миль");
            }
            return h;
        }

        /**
         * Виводить стовпці даних одного рядка.
         *
         * @param r   об'єкт даних
         * @param cfg конфігурація
         * @param w   ширина стовпця
         * @param sep символ-роздільник
         */
        protected void printDataCols(BallisticData r, TableConfig cfg, int w, char sep) {
            if (cfg.isShowMetric()) {
                System.out.printf(" %-" + w + ".2f %s", r.getRangeMetric(), sep);
                if (cfg.isShowDerived())
                    System.out.printf(" %-" + w + ".4f %s", r.getRangeMetric() / 1000.0, sep);
            }
            if (cfg.isShowImperial()) {
                System.out.printf(" %-" + w + ".2f %s", r.getRangeImperial(), sep);
                if (cfg.isShowDerived())
                    System.out.printf(" %-" + w + ".4f %s", r.getRangeImperial() / 5280.0, sep);
            }
        }
    }

    // =========================================================================
    // BorderedTableDisplay — другий рівень перевизначення
    // =========================================================================

    /**
     * Табличне відображення з рамкою з псевдографічних символів.
     *
     * <p><b>Розширює {@link SimpleTableDisplay}</b>, демонструючи
     * дворівневий ланцюг <b>overriding</b>:</p>
     * <pre>
     *   TextDisplay.displayTable()
     *     ← SimpleTableDisplay.displayTable()   (1-й рівень overriding)
     *       ← BorderedTableDisplay.displayTable() (2-й рівень overriding)
     * </pre>
     *
     * @see BorderedTableDisplayFactory
     */
    static class BorderedTableDisplay extends SimpleTableDisplay {

        /**
         * Символи рамки: ┌ ┐ └ ┘ ├ ┤ ┬ ┴ ┼ ─ │.
         */
        private static final String TL = "+", TR = "+", BL = "+", BR = "+";
        private static final String ML = "+", MR = "+";
        private static final String H  = "-", V  = "|";

        /**
         * Створює об'єкт з конфігурацією за замовчуванням.
         *
         * @param data дані для відображення
         */
        public BorderedTableDisplay(BallisticData data) {
            super(data);
        }

        /**
         * Створює об'єкт із заданою конфігурацією.
         *
         * @param data   дані для відображення
         * @param config параметри таблиці
         */
        public BorderedTableDisplay(BallisticData data, TableConfig config) {
            super(data, config);
        }

        /**
         * Виводить короткий рядок з маркером «[Bordered]».
         * <p><b>Overriding</b> методу {@link SimpleTableDisplay#displayShort()}
         * (2-й рівень).</p>
         */
        @Override
        public void displayShort() {
            System.out.printf("[Bordered] v0=%.1f м/с | α=%.1f° | %.2f м | %.2f фут%n",
                data.getInitialVelocity(), data.getAngleDegrees(),
                data.getRangeMetric(), data.getRangeImperial());
        }

        /**
         * Виводить таблицю з рамкою.
         * <p><b>Overriding</b> методу {@link SimpleTableDisplay#displayTable(List, TableConfig)}
         * (2-й рівень).</p>
         *
         * @param results список результатів
         * @param cfg     конфігурація таблиці
         */
        @Override
        public void displayTable(List<BallisticData> results, TableConfig cfg) {
            int w    = cfg.getColumnWidth();
            int rows = cfg.effectiveRows(results.size());

            List<String> headers = buildHeaders(cfg);
            // Ширини: № + v0 + α + data cols
            int[] widths = new int[2 + headers.size()];
            widths[0] = 3; widths[1] = Math.max(w, 8); widths[2] = Math.max(w, 6);
            for (int i = 3; i < widths.length; i++) widths[i] = w;

            String top    = buildHLine(TL, TR, H, widths);
            String mid    = buildHLine(ML, MR, H, widths);
            String bottom = buildHLine(BL, BR, H, widths);

            // Заголовок таблиці
            int totalWidth = Arrays.stream(widths).sum() + widths.length * 3 + 1;
            System.out.println(top);
            System.out.printf("%s %-" + (totalWidth - 2) + "s %s%n", V, cfg.getTitle(), V);
            System.out.println(mid);

            // Рядок заголовків колонок
            System.out.printf("%s %-3s %s", V, "№", V);
            System.out.printf(" %-" + Math.max(w, 8) + "s %s", "v0(м/с)", V);
            System.out.printf(" %-" + Math.max(w, 6) + "s %s", "α(°)", V);
            for (String h : headers)
                System.out.printf(" %-" + w + "s %s", h, V);
            System.out.println();
            System.out.println(mid);

            // Рядки даних
            if (results.isEmpty()) {
                System.out.printf("%s %-" + (totalWidth - 2) + "s %s%n", V, "  [порожньо]", V);
            } else {
                for (int i = 0; i < rows; i++) {
                    BallisticData r = results.get(i);
                    System.out.printf("%s %-3d %s", V, i + 1, V);
                    System.out.printf(" %-" + Math.max(w, 8) + ".2f %s", r.getInitialVelocity(), V);
                    System.out.printf(" %-" + Math.max(w, 6) + ".2f %s", r.getAngleDegrees(), V);
                    printDataCols(r, cfg, w, V.charAt(0));
                    System.out.println();
                }
                if (rows < results.size())
                    System.out.printf("%s ... ще %d запис(ів) %s%n", V, results.size() - rows, V);
            }
            System.out.println(bottom);
        }

        /**
         * Будує горизонтальний рядок рамки за масивом ширин.
         *
         * @param left   лівий символ
         * @param right  правий символ
         * @param horiz  горизонтальний символ
         * @param widths ширини стовпців
         * @return рядок-рамка
         */
        private static String buildHLine(String left, String right,
                                         String horiz, int[] widths) {
            StringBuilder sb = new StringBuilder(left);
            for (int i = 0; i < widths.length; i++) {
                sb.append(horiz.repeat(widths[i] + 2));
                sb.append(i < widths.length - 1 ? "+" : right);
            }
            return sb.toString();
        }
    }

    // =========================================================================
    // DetailedTextDisplay — конкретний продукт
    // =========================================================================

    /**
     * Детальне текстове відображення з формулою та проміжними обчисленнями.
     *
     * <p>Конкретний <b>Product</b> у шаблоні Factory Method.
     * Не входить до ієрархії таблиць — незалежна реалізація.</p>
     *
     * @see DetailedDisplayFactory
     */
    static class DetailedTextDisplay implements Displayable {

        /** Дані для відображення. */
        private final BallisticData data;

        /**
         * Створює детальний об'єкт відображення.
         *
         * @param data об'єкт {@link BallisticData}
         */
        public DetailedTextDisplay(BallisticData data) {
            this.data = data;
        }

        /** Виводить короткий рядок із позначкою «Детально». */
        @Override
        public void displayShort() {
            System.out.printf("[Детально] v0=%.1f м/с | α=%.1f° | %.4f м | %.4f фут%n",
                data.getInitialVelocity(), data.getAngleDegrees(),
                data.getRangeMetric(), data.getRangeImperial());
        }

        /** Виводить повний детальний звіт із формулою. */
        @Override
        public void displayFull() {
            double v0       = data.getInitialVelocity();
            double alpha    = data.getAngleDegrees();
            double alphaRad = Math.toRadians(alpha);
            double sin2a    = Math.sin(2.0 * alphaRad);
            System.out.println("====== Детальний звіт (DetailedTextDisplay) ======");
            System.out.println("  Формула: s = v0² · sin(2α) / g");
            System.out.printf("    v0        = %.4f м/с%n",    v0);
            System.out.printf("    α         = %.4f° (%.6f рад)%n", alpha, alphaRad);
            System.out.printf("    v0²       = %.4f%n",         v0 * v0);
            System.out.printf("    sin(2α)   = %.6f%n",         sin2a);
            System.out.printf("    Метрична  : %.6f м%n",       data.getRangeMetric());
            System.out.printf("    Англійська: %.6f фут%n",     data.getRangeImperial());
            System.out.println("==================================================");
        }

        /** Виводить таблицю з усіма значеннями. */
        @Override
        public void displayTable(List<BallisticData> results) {
            System.out.println("====== Детальна таблиця (DetailedTextDisplay) ======");
            System.out.printf("  %-4s %-10s %-8s %-14s %-14s%n",
                "№", "v0 (м/с)", "α (°)", "Метрично (м)", "Англійськ (фут)");
            System.out.println("  " + "-".repeat(54));
            if (results.isEmpty()) {
                System.out.println("  [порожньо]");
            } else {
                for (int i = 0; i < results.size(); i++) {
                    BallisticData r = results.get(i);
                    System.out.printf("  %-4d %-10.2f %-8.2f %-14.4f %-14.4f%n",
                        i + 1, r.getInitialVelocity(), r.getAngleDegrees(),
                        r.getRangeMetric(), r.getRangeImperial());
                }
            }
            System.out.println("  " + "=".repeat(54));
        }
    }

    // =========================================================================
    // CompactDisplay — конкретний продукт
    // =========================================================================

    /**
     * Компактне однорядкове відображення результатів.
     *
     * <p>Конкретний <b>Product</b> у шаблоні Factory Method.</p>
     *
     * @see CompactDisplayFactory
     */
    static class CompactDisplay implements Displayable {

        /** Дані для відображення. */
        private final BallisticData data;

        /**
         * Створює компактний об'єкт відображення.
         *
         * @param data об'єкт {@link BallisticData}
         */
        public CompactDisplay(BallisticData data) {
            this.data = data;
        }

        /** Виводить ультра-короткий рядок. */
        @Override
        public void displayShort() {
            System.out.printf("[Компакт] %.0f м/с @ %.0f° -> %.0f м%n",
                data.getInitialVelocity(), data.getAngleDegrees(), data.getRangeMetric());
        }

        /** Виводить компактний однорядковий звіт. */
        @Override
        public void displayFull() {
            System.out.printf("[ CompactDisplay ] v0=%.2f | α=%.2f° | " +
                              "s=%.2f м | s=%.2f фут%n",
                data.getInitialVelocity(), data.getAngleDegrees(),
                data.getRangeMetric(), data.getRangeImperial());
        }

        /** Виводить всі результати в одну колонку. */
        @Override
        public void displayTable(List<BallisticData> results) {
            System.out.printf("[Компактна таблиця | %d записів]%n", results.size());
            for (int i = 0; i < results.size(); i++) {
                BallisticData r = results.get(i);
                System.out.printf("  #%-2d %.0f м/с @ %.0f° -> %.0f м%n",
                    i + 1, r.getInitialVelocity(), r.getAngleDegrees(), r.getRangeMetric());
            }
        }
    }

    // =========================================================================
    // CsvTableDisplay — незалежна реалізація ConfigurableDisplay
    // =========================================================================

    /**
     * Відображення результатів у форматі CSV (comma-separated values).
     *
     * <p>Реалізує {@link ConfigurableDisplay} незалежно, без наслідування
     * {@link TextDisplay}. Розширює ієрархію Factory Method новим типом продукту.</p>
     *
     * <p>Демонстрація <b>overloading</b>: три версії {@code displayTable}
     * з різними сигнатурами.</p>
     *
     * @see CsvTableDisplayFactory
     */
    static class CsvTableDisplay implements ConfigurableDisplay {

        /** Дані для відображення. */
        private final BallisticData data;

        /** Конфігурація (використовується роздільник як CSV-delimiter). */
        private TableConfig config;

        /**
         * Створює CSV-відображення з конфігурацією за замовчуванням.
         *
         * @param data дані для відображення
         */
        public CsvTableDisplay(BallisticData data) {
            this.data   = data;
            this.config = new TableConfig();
            this.config.setSeparator(',');
        }

        /**
         * Створює CSV-відображення із заданою конфігурацією.
         *
         * @param data   дані для відображення
         * @param config конфігурація (роздільник береться як CSV-delimiter)
         */
        public CsvTableDisplay(BallisticData data, TableConfig config) {
            this.data   = data;
            this.config = (config != null) ? config : new TableConfig();
        }

        /** Виводить короткий CSV-рядок з даними. */
        @Override
        public void displayShort() {
            char d = config.getSeparator();
            System.out.printf("[CSV] %.2f%c%.2f%c%.4f%c%.4f%n",
                data.getInitialVelocity(), d, data.getAngleDegrees(), d,
                data.getRangeMetric(), d, data.getRangeImperial());
        }

        /** Виводить заголовок + один CSV-рядок. */
        @Override
        public void displayFull() {
            char d = config.getSeparator();
            System.out.println("# CsvTableDisplay — один запис");
            System.out.printf("v0%cAlpha%c", d, d);
            if (config.isShowMetric())   System.out.printf("metric_m%c",   d);
            if (config.isShowImperial()) System.out.printf("imperial_ft%c", d);
            System.out.println();
            System.out.printf("%.4f%c%.4f%c", data.getInitialVelocity(), d, data.getAngleDegrees(), d);
            if (config.isShowMetric())   System.out.printf("%.4f%c", data.getRangeMetric(),   d);
            if (config.isShowImperial()) System.out.printf("%.4f%c", data.getRangeImperial(), d);
            System.out.println();
        }

        /**
         * Виводить всі результати у CSV.
         * <p><b>Overloading #1</b> (базовий варіант).</p>
         *
         * @param results список результатів
         */
        @Override
        public void displayTable(List<BallisticData> results) {
            displayTable(results, config);
        }

        /**
         * Виводить не більше {@code maxRows} рядків CSV.
         * <p><b>Overloading #2</b>.</p>
         *
         * @param results список результатів
         * @param maxRows максимальна кількість рядків
         */
        @Override
        public void displayTable(List<BallisticData> results, int maxRows) {
            TableConfig tmp = new TableConfig(
                config.getColumnWidth(), config.getSeparator(),
                config.isShowMetric(), config.isShowImperial(),
                maxRows, config.getTitle(), config.isShowDerived());
            displayTable(results, tmp);
        }

        /**
         * Виводить CSV з повною конфігурацією.
         * <p><b>Overloading #3</b>.</p>
         *
         * @param results список результатів
         * @param cfg     конфігурація таблиці
         */
        @Override
        public void displayTable(List<BallisticData> results, TableConfig cfg) {
            char d    = cfg.getSeparator();
            int  rows = cfg.effectiveRows(results.size());

            System.out.println("# " + cfg.getTitle() + " [CSV]");
            // Заголовок CSV
            System.out.printf("no%cv0%calpha%c", d, d, d);
            if (cfg.isShowMetric())   System.out.printf("metric_m%c",   d);
            if (cfg.isShowDerived() && cfg.isShowMetric()) System.out.printf("metric_km%c", d);
            if (cfg.isShowImperial()) System.out.printf("imperial_ft%c", d);
            if (cfg.isShowDerived() && cfg.isShowImperial()) System.out.printf("imperial_mi%c", d);
            System.out.println();

            for (int i = 0; i < rows; i++) {
                BallisticData r = results.get(i);
                System.out.printf("%d%c%.4f%c%.4f%c", i + 1, d,
                    r.getInitialVelocity(), d, r.getAngleDegrees(), d);
                if (cfg.isShowMetric())   System.out.printf("%.4f%c", r.getRangeMetric(),   d);
                if (cfg.isShowDerived() && cfg.isShowMetric())
                    System.out.printf("%.6f%c", r.getRangeMetric() / 1000.0, d);
                if (cfg.isShowImperial()) System.out.printf("%.4f%c", r.getRangeImperial(), d);
                if (cfg.isShowDerived() && cfg.isShowImperial())
                    System.out.printf("%.6f%c", r.getRangeImperial() / 5280.0, d);
                System.out.println();
            }
            if (rows < results.size())
                System.out.printf("# ... ще %d записів пропущено%n", results.size() - rows);
        }
    }

    // =========================================================================
    // Конкретні фабрики
    // =========================================================================

    /**
     * Фабрика для {@link TextDisplay}.
     * Конкретний <b>Creator</b> у шаблоні Factory Method.
     */
    static class TextDisplayFactory implements DisplayFactory {
        /** {@inheritDoc} */
        @Override
        public Displayable createDisplay(BallisticData data) {
            return new TextDisplay(data);
        }
    }

    /**
     * Фабрика для {@link DetailedTextDisplay}.
     * Конкретний <b>Creator</b> у шаблоні Factory Method.
     */
    static class DetailedDisplayFactory implements DisplayFactory {
        /** {@inheritDoc} */
        @Override
        public Displayable createDisplay(BallisticData data) {
            return new DetailedTextDisplay(data);
        }
    }

    /**
     * Фабрика для {@link CompactDisplay}.
     * Конкретний <b>Creator</b> у шаблоні Factory Method.
     */
    static class CompactDisplayFactory implements DisplayFactory {
        /** {@inheritDoc} */
        @Override
        public Displayable createDisplay(BallisticData data) {
            return new CompactDisplay(data);
        }
    }

    /**
     * Фабрика для {@link SimpleTableDisplay} з конфігурацією за замовчуванням.
     * Конкретний <b>Creator</b> у шаблоні Factory Method.
     *
     * @see SimpleTableDisplay
     */
    static class SimpleTableDisplayFactory implements DisplayFactory {

        /** Конфігурація, що передається при створенні. */
        private final TableConfig config;

        /**
         * Створює фабрику з конфігурацією за замовчуванням.
         */
        public SimpleTableDisplayFactory() {
            this.config = new TableConfig();
        }

        /**
         * Створює фабрику із заданою конфігурацією.
         *
         * @param config конфігурація таблиці
         */
        public SimpleTableDisplayFactory(TableConfig config) {
            this.config = (config != null) ? config : new TableConfig();
        }

        /** {@inheritDoc} */
        @Override
        public Displayable createDisplay(BallisticData data) {
            return new SimpleTableDisplay(data, config);
        }
    }

    /**
     * Фабрика для {@link BorderedTableDisplay}.
     * Конкретний <b>Creator</b> у шаблоні Factory Method.
     *
     * @see BorderedTableDisplay
     */
    static class BorderedTableDisplayFactory implements DisplayFactory {

        /** Конфігурація, що передається при створенні. */
        private final TableConfig config;

        /**
         * Створює фабрику з конфігурацією за замовчуванням.
         */
        public BorderedTableDisplayFactory() {
            this.config = new TableConfig();
        }

        /**
         * Створює фабрику із заданою конфігурацією.
         *
         * @param config конфігурація таблиці
         */
        public BorderedTableDisplayFactory(TableConfig config) {
            this.config = (config != null) ? config : new TableConfig();
        }

        /** {@inheritDoc} */
        @Override
        public Displayable createDisplay(BallisticData data) {
            return new BorderedTableDisplay(data, config);
        }
    }

    /**
     * Фабрика для {@link CsvTableDisplay}.
     * Конкретний <b>Creator</b> у шаблоні Factory Method.
     *
     * @see CsvTableDisplay
     */
    static class CsvTableDisplayFactory implements DisplayFactory {

        /** Конфігурація, що передається при створенні. */
        private final TableConfig config;

        /**
         * Створює фабрику з конфігурацією за замовчуванням.
         */
        public CsvTableDisplayFactory() {
            TableConfig cfg = new TableConfig();
            cfg.setSeparator(',');
            this.config = cfg;
        }

        /**
         * Створює фабрику із заданою конфігурацією.
         *
         * @param config конфігурація (роздільник = CSV-delimiter)
         */
        public CsvTableDisplayFactory(TableConfig config) {
            this.config = (config != null) ? config : new TableConfig();
        }

        /** {@inheritDoc} */
        @Override
        public Displayable createDisplay(BallisticData data) {
            return new CsvTableDisplay(data, config);
        }
    }

    // =========================================================================
    // BallisticTest
    // =========================================================================

    /**
     * Клас для тестування коректності обчислень, серіалізації, колекції,
     * Factory Method, overriding, overloading та поліморфізму.
     *
     * <p>Тести організовані в групи:</p>
     * <ol>
     *   <li>Граничні кути та відомі значення.</li>
     *   <li>Серіалізація та transient-поля.</li>
     *   <li>Валідація вхідних даних.</li>
     *   <li>Колекція ResultCollection.</li>
     *   <li>Фабрики та ієрархія класів.</li>
     *   <li>TableConfig.</li>
     *   <li>Overloading методів displayTable.</li>
     *   <li>Overriding у ієрархії.</li>
     *   <li>Поліморфізм (dynamic method dispatch).</li>
     * </ol>
     */
    static class BallisticTest {

        /** Допустима похибка для порівняння дійсних чисел. */
        private static final double EPSILON = 1e-6;

        /** Ім'я тимчасового файлу для тестів серіалізації. */
        private static final String TEST_FILE = "test_ballistic.ser";

        /** Ім'я тимчасового файлу для тестів колекції. */
        private static final String TEST_COLL = "test_collection.ser";

        /** Лічильник пройдених тестів. */
        private int passed = 0;

        /** Лічильник провалених тестів. */
        private int failed = 0;

        /**
         * Запускає всі тести та повертає {@code true}, якщо всі пройдені.
         *
         * @return {@code true} якщо всі тести пройшли
         */
        public boolean runAll() {
            System.out.println("+======================================================+");
            System.out.println("|          Тестування — BallisticApp v3.0              |");
            System.out.println("+======================================================+");

            System.out.println("\n--- Група 1: Обчислення ---");
            testAngle45();
            testAngle0();
            testAngle90();
            testKnownValues();
            testMetricImperialConsistency();

            System.out.println("\n--- Група 2: Серіалізація ---");
            testSerializationRegularFields();
            testTransientFieldsLost();

            System.out.println("\n--- Група 3: Валідація ---");
            testInvalidVelocity();
            testInvalidAngle();
            testNullData();

            System.out.println("\n--- Група 4: ResultCollection ---");
            testCollectionAddAndSize();
            testCollectionSaveLoad();
            testCollectionClear();

            System.out.println("\n--- Група 5: Фабрики та ієрархія ---");
            testTextDisplayFactory();
            testDetailedDisplayFactory();
            testCompactDisplayFactory();
            testSimpleTableDisplayFactory();
            testBorderedTableDisplayFactory();
            testCsvTableDisplayFactory();

            System.out.println("\n--- Група 6: TableConfig ---");
            testTableConfigDefaults();
            testTableConfigCustom();
            testTableConfigEffectiveRows();
            testTableConfigColumnWidth();

            System.out.println("\n--- Група 7: Overloading ---");
            testOverloadingByMaxRows();
            testOverloadingByConfig();
            testCsvOverloadingByMaxRows();
            testCsvOverloadingByConfig();

            System.out.println("\n--- Група 8: Overriding ---");
            testOverridingSimpleVsText();
            testOverridingBorderedVsSimple();

            System.out.println("\n--- Група 9: Поліморфізм (dynamic dispatch) ---");
            testPolymorphismDisplayTable();
            testPolymorphismDisplayShort();

            System.out.println("\n==============================================");
            System.out.printf("  Результат: %d пройдено, %d провалено з %d%n",
                passed, failed, passed + failed);
            System.out.println("==============================================");

            new File(TEST_FILE).delete();
            new File(TEST_COLL).delete();
            return failed == 0;
        }

        // --- Група 1 ---

        /** Кут 45° → максимальна дальність {@code s = v0²/g}. */
        private void testAngle45() {
            double v0 = 100.0;
            assertAlmostEqual("Кут 45° — максимальна дальність",
                (v0 * v0) / BallisticData.G_METRIC,
                new BallisticSolver(v0, 45.0).computeRangeMetric());
        }

        /** Кут 0° → дальність = 0. */
        private void testAngle0() {
            assertAlmostEqual("Кут 0° — дальність = 0",
                0.0, new BallisticSolver(100.0, 0.0).computeRangeMetric());
        }

        /** Кут 90° → дальність ≈ 0. */
        private void testAngle90() {
            assertAlmostEqual("Кут 90° — дальність ≈ 0",
                0.0, new BallisticSolver(100.0, 90.0).computeRangeMetric());
        }

        /** v0=200 м/с, α=30° → перевірка за формулою. */
        private void testKnownValues() {
            double v0 = 200.0;
            double expected = (v0 * v0 * Math.sin(Math.toRadians(60.0))) / BallisticData.G_METRIC;
            BallisticSolver s = new BallisticSolver(v0, 30.0);
            s.solve();
            assertAlmostEqual("Відомі значення (v0=200, α=30°)", expected, s.getData().getRangeMetric());
        }

        /** Узгодженість метрична/англійська < 0.5%. */
        private void testMetricImperialConsistency() {
            BallisticSolver s = new BallisticSolver(150.0, 60.0);
            s.solve();
            double mf  = s.getData().getRangeMetric() * BallisticData.METERS_TO_FEET;
            double imp = s.getData().getRangeImperial();
            double rel = Math.abs(mf - imp) / imp;
            report("Узгодженість метрична/англійська (< 0.5%)", rel < 0.005,
                String.format("відн.похибка=%.6f", rel));
        }

        // --- Група 2 ---

        /** Звичайні поля зберігаються після серіалізації. */
        private void testSerializationRegularFields() {
            try {
                BallisticData orig = new BallisticData(250.0, 35.0);
                new BallisticSolver(orig).solve();
                try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(TEST_FILE))) {
                    o.writeObject(orig);
                }
                BallisticData rest;
                try (ObjectInputStream i = new ObjectInputStream(new FileInputStream(TEST_FILE))) {
                    rest = (BallisticData) i.readObject();
                }
                boolean ok = orig.getInitialVelocity() == rest.getInitialVelocity()
                          && orig.getAngleDegrees()    == rest.getAngleDegrees()
                          && orig.getRangeMetric()     == rest.getRangeMetric()
                          && orig.getRangeImperial()   == rest.getRangeImperial();
                report("Серіалізація — збереження звичайних полів", ok, "");
            } catch (Exception e) { report("Серіалізація", false, e.getMessage()); }
        }

        /** Transient-поля скидаються після десеріалізації. */
        private void testTransientFieldsLost() {
            try {
                BallisticData orig = new BallisticData(100.0, 45.0);
                try (ObjectOutputStream o = new ObjectOutputStream(new FileOutputStream(TEST_FILE))) {
                    o.writeObject(orig);
                }
                BallisticData rest;
                try (ObjectInputStream i = new ObjectInputStream(new FileInputStream(TEST_FILE))) {
                    rest = (BallisticData) i.readObject();
                }
                report("Transient: timestamp → 0",   rest.getTimestamp() == 0L, "отримано: " + rest.getTimestamp());
                report("Transient: status → null", rest.getStatusMessage() == null, "отримано: " + rest.getStatusMessage());
            } catch (Exception e) { report("Transient", false, e.getMessage()); }
        }

        // --- Група 3 ---

        /** Від'ємна швидкість → виняток. */
        private void testInvalidVelocity() {
            try { new BallisticData(-10.0, 45.0);
                  report("Від'ємна швидкість → виняток", false, "не кинуто"); }
            catch (IllegalArgumentException e) { report("Від'ємна швидкість → виняток", true, e.getMessage()); }
        }

        /** Кут поза [0;90] → виняток. */
        private void testInvalidAngle() {
            boolean c1 = false, c2 = false;
            try { new BallisticData(100.0, -5.0);  } catch (IllegalArgumentException e) { c1 = true; }
            try { new BallisticData(100.0, 100.0); } catch (IllegalArgumentException e) { c2 = true; }
            report("Некоректний кут → виняток", c1 && c2, "");
        }

        /** null → виняток у Solver. */
        private void testNullData() {
            try { new BallisticSolver(null);
                  report("Null data → виняток", false, "не кинуто"); }
            catch (IllegalArgumentException e) { report("Null data → виняток", true, e.getMessage()); }
        }

        // --- Група 4 ---

        /** Додавання та розмір колекції. */
        private void testCollectionAddAndSize() {
            ResultCollection c = new ResultCollection();
            c.add(new BallisticData(100.0, 30.0));
            c.add(new BallisticData(200.0, 45.0));
            report("ResultCollection: розмір=2", c.size() == 2, "розмір=" + c.size());
        }

        /** Збереження та відновлення колекції. */
        private void testCollectionSaveLoad() {
            try {
                ResultCollection orig = new ResultCollection();
                BallisticData d1 = new BallisticData(100.0, 30.0);
                BallisticData d2 = new BallisticData(200.0, 45.0);
                new BallisticSolver(d1).solve(); new BallisticSolver(d2).solve();
                orig.add(d1); orig.add(d2);
                orig.saveToFile(TEST_COLL);
                ResultCollection rest = new ResultCollection();
                rest.loadFromFile(TEST_COLL);
                boolean ok = rest.size() == 2
                    && rest.getAll().get(0).getInitialVelocity() == 100.0
                    && rest.getAll().get(1).getAngleDegrees()    == 45.0;
                report("ResultCollection: save/load", ok, "розмір=" + rest.size());
            } catch (Exception e) { report("ResultCollection: save/load", false, e.getMessage()); }
        }

        /** Очищення колекції. */
        private void testCollectionClear() {
            ResultCollection c = new ResultCollection();
            c.add(new BallisticData(100.0, 45.0));
            c.clear();
            report("ResultCollection: clear", c.size() == 0, "розмір=" + c.size());
        }

        // --- Група 5 ---

        /** TextDisplayFactory → TextDisplay. */
        private void testTextDisplayFactory() {
            Displayable d = new TextDisplayFactory().createDisplay(makeSolvedData());
            report("TextDisplayFactory → TextDisplay", d instanceof TextDisplay, d.getClass().getSimpleName());
        }

        /** DetailedDisplayFactory → DetailedTextDisplay. */
        private void testDetailedDisplayFactory() {
            Displayable d = new DetailedDisplayFactory().createDisplay(makeSolvedData());
            report("DetailedDisplayFactory → DetailedTextDisplay",
                d instanceof DetailedTextDisplay, d.getClass().getSimpleName());
        }

        /** CompactDisplayFactory → CompactDisplay. */
        private void testCompactDisplayFactory() {
            Displayable d = new CompactDisplayFactory().createDisplay(makeSolvedData());
            report("CompactDisplayFactory → CompactDisplay", d instanceof CompactDisplay, d.getClass().getSimpleName());
        }

        /** SimpleTableDisplayFactory → SimpleTableDisplay. */
        private void testSimpleTableDisplayFactory() {
            Displayable d = new SimpleTableDisplayFactory().createDisplay(makeSolvedData());
            report("SimpleTableDisplayFactory → SimpleTableDisplay",
                d instanceof SimpleTableDisplay, d.getClass().getSimpleName());
        }

        /** BorderedTableDisplayFactory → BorderedTableDisplay. */
        private void testBorderedTableDisplayFactory() {
            Displayable d = new BorderedTableDisplayFactory().createDisplay(makeSolvedData());
            report("BorderedTableDisplayFactory → BorderedTableDisplay",
                d instanceof BorderedTableDisplay, d.getClass().getSimpleName());
        }

        /** CsvTableDisplayFactory → CsvTableDisplay. */
        private void testCsvTableDisplayFactory() {
            Displayable d = new CsvTableDisplayFactory().createDisplay(makeSolvedData());
            report("CsvTableDisplayFactory → CsvTableDisplay",
                d instanceof CsvTableDisplay, d.getClass().getSimpleName());
        }

        // --- Група 6 ---

        /** TableConfig за замовчуванням. */
        private void testTableConfigDefaults() {
            TableConfig cfg = new TableConfig();
            boolean ok = cfg.getColumnWidth() == 10
                      && cfg.getSeparator()   == '|'
                      && cfg.isShowMetric()
                      && cfg.isShowImperial()
                      && cfg.getMaxRows()     == 0
                      && !cfg.isShowDerived();
            report("TableConfig: значення за замовчуванням", ok, cfg.toString());
        }

        /** TableConfig з кастомними параметрами. */
        private void testTableConfigCustom() {
            TableConfig cfg = new TableConfig(14, ';', true, false, 5, "Тест", true);
            boolean ok = cfg.getColumnWidth() == 14
                      && cfg.getSeparator()   == ';'
                      && cfg.isShowMetric()
                      && !cfg.isShowImperial()
                      && cfg.getMaxRows()     == 5
                      && cfg.isShowDerived()
                      && cfg.getTitle().equals("Тест");
            report("TableConfig: кастомні параметри", ok, cfg.toString());
        }

        /** effectiveRows: обмеження maxRows. */
        private void testTableConfigEffectiveRows() {
            TableConfig cfg = new TableConfig();
            cfg.setMaxRows(3);
            boolean ok = cfg.effectiveRows(10) == 3
                      && cfg.effectiveRows(2)  == 2;
            report("TableConfig: effectiveRows", ok, "");
        }

        /** Мінімальна ширина стовпця = 4. */
        private void testTableConfigColumnWidth() {
            TableConfig cfg = new TableConfig();
            cfg.setColumnWidth(1);
            report("TableConfig: мін.ширина=4", cfg.getColumnWidth() == 4,
                "ширина=" + cfg.getColumnWidth());
        }

        // --- Група 7: Overloading ---

        /**
         * Overloading: displayTable(List, int) обмежує кількість рядків.
         */
        private void testOverloadingByMaxRows() {
            SimpleTableDisplay st = new SimpleTableDisplay(makeSolvedData());
            List<BallisticData> list = makeSolvedList(5);
            // Тест через StringPrintStream — перевіряємо що не кидає виняток
            try {
                PrintStream old = System.out;
                System.setOut(new PrintStream(OutputStream.nullOutputStream()));
                st.displayTable(list, 3);          // overload int
                System.setOut(old);
                report("Overloading: displayTable(List, int)", true, "");
            } catch (Exception e) {
                report("Overloading: displayTable(List, int)", false, e.getMessage());
            }
        }

        /**
         * Overloading: displayTable(List, TableConfig) використовує конфігурацію.
         */
        private void testOverloadingByConfig() {
            SimpleTableDisplay st = new SimpleTableDisplay(makeSolvedData());
            TableConfig cfg = new TableConfig(8, '+', true, false, 0, "Тест", false);
            List<BallisticData> list = makeSolvedList(3);
            try {
                PrintStream old = System.out;
                System.setOut(new PrintStream(OutputStream.nullOutputStream()));
                st.displayTable(list, cfg);        // overload TableConfig
                System.setOut(old);
                report("Overloading: displayTable(List, TableConfig)", true, "");
            } catch (Exception e) {
                report("Overloading: displayTable(List, TableConfig)", false, e.getMessage());
            }
        }

        /**
         * Overloading: CsvTableDisplay.displayTable(List, int).
         */
        private void testCsvOverloadingByMaxRows() {
            CsvTableDisplay csv = new CsvTableDisplay(makeSolvedData());
            try {
                PrintStream old = System.out;
                System.setOut(new PrintStream(OutputStream.nullOutputStream()));
                csv.displayTable(makeSolvedList(5), 2);
                System.setOut(old);
                report("Overloading CSV: displayTable(List, int)", true, "");
            } catch (Exception e) {
                report("Overloading CSV: displayTable(List, int)", false, e.getMessage());
            }
        }

        /**
         * Overloading: CsvTableDisplay.displayTable(List, TableConfig).
         */
        private void testCsvOverloadingByConfig() {
            CsvTableDisplay csv = new CsvTableDisplay(makeSolvedData());
            TableConfig cfg = new TableConfig(8, ';', true, true, 0, "CSV", true);
            try {
                PrintStream old = System.out;
                System.setOut(new PrintStream(OutputStream.nullOutputStream()));
                csv.displayTable(makeSolvedList(3), cfg);
                System.setOut(old);
                report("Overloading CSV: displayTable(List, TableConfig)", true, "");
            } catch (Exception e) {
                report("Overloading CSV: displayTable(List, TableConfig)", false, e.getMessage());
            }
        }

        // --- Група 8: Overriding ---

        /**
         * Overriding: SimpleTableDisplay перевизначає displayShort() TextDisplay.
         * Перевірка через instanceof та відмінність класів.
         */
        private void testOverridingSimpleVsText() {
            BallisticData d   = makeSolvedData();
            TextDisplay   td  = new TextDisplay(d);
            SimpleTableDisplay std = new SimpleTableDisplay(d);
            // Обидва — Displayable, але std — підклас td
            boolean isSubclass = std instanceof TextDisplay;
            // Але displayTable() — перевизначений
            boolean diffClass  = !td.getClass().equals(std.getClass());
            report("Overriding: SimpleTableDisplay extends TextDisplay",
                isSubclass && diffClass, "");
        }

        /**
         * Overriding: BorderedTableDisplay перевизначає SimpleTableDisplay.
         * 2-й рівень ланцюга overriding.
         */
        private void testOverridingBorderedVsSimple() {
            BallisticData d  = makeSolvedData();
            BorderedTableDisplay bd = new BorderedTableDisplay(d);
            // BorderedTableDisplay IS-A SimpleTableDisplay IS-A TextDisplay
            boolean isSimple = bd instanceof SimpleTableDisplay;
            boolean isText   = bd instanceof TextDisplay;
            boolean isDisp   = bd instanceof Displayable;
            report("Overriding: BorderedTableDisplay extends SimpleTableDisplay",
                isSimple && isText && isDisp, "");
        }

        // --- Група 9: Поліморфізм ---

        /**
         * Поліморфізм (dynamic method dispatch): виклик displayTable через
         * {@link Displayable}-посилання викликає перевизначений метод реального класу.
         */
        private void testPolymorphismDisplayTable() {
            List<BallisticData> list = makeSolvedList(2);
            BallisticData d = makeSolvedData();

            // Масив різних реалізацій — усі через Displayable
            Displayable[] displays = {
                new TextDisplay(d),
                new SimpleTableDisplay(d),
                new BorderedTableDisplay(d),
                new DetailedTextDisplay(d),
                new CompactDisplay(d),
                new CsvTableDisplay(d)
            };

            try {
                PrintStream old = System.out;
                System.setOut(new PrintStream(OutputStream.nullOutputStream()));
                for (Displayable disp : displays) {
                    // Пізнє зв'язування: який метод викликати визначається в runtime
                    disp.displayTable(list);
                }
                System.setOut(old);
                report("Поліморфізм: displayTable через Displayable[] (6 типів)", true, "");
            } catch (Exception e) {
                report("Поліморфізм: displayTable через Displayable[]", false, e.getMessage());
            }
        }

        /**
         * Поліморфізм: displayShort() через Displayable — кожен клас виводить по-своєму.
         */
        private void testPolymorphismDisplayShort() {
            BallisticData d = makeSolvedData();
            Displayable[] displays = {
                new TextDisplay(d),
                new SimpleTableDisplay(d),
                new BorderedTableDisplay(d),
                new CompactDisplay(d)
            };
            // Перевіряємо що всі викликаються без виняток через інтерфейсне посилання
            try {
                PrintStream old = System.out;
                System.setOut(new PrintStream(OutputStream.nullOutputStream()));
                for (Displayable disp : displays)
                    disp.displayShort();
                System.setOut(old);
                report("Поліморфізм: displayShort() через Displayable (4 типи)", true, "");
            } catch (Exception e) {
                report("Поліморфізм: displayShort()", false, e.getMessage());
            }
        }

        // --- Допоміжні методи ---

        /**
         * Створює та розв'язує об'єкт {@link BallisticData} для тестів.
         *
         * @return розв'язаний об'єкт
         */
        private BallisticData makeSolvedData() {
            BallisticData d = new BallisticData(100.0, 45.0);
            new BallisticSolver(d).solve();
            return d;
        }

        /**
         * Створює список з {@code n} розв'язаних об'єктів для тестів.
         *
         * @param n кількість елементів
         * @return список {@link BallisticData}
         */
        private List<BallisticData> makeSolvedList(int n) {
            List<BallisticData> list = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                BallisticData d = new BallisticData(50.0 + i * 30.0, 15.0 + i * 10.0);
                new BallisticSolver(d).solve();
                list.add(d);
            }
            return list;
        }

        /** Порівнює два числа з похибкою {@link #EPSILON}. */
        private void assertAlmostEqual(String name, double expected, double actual) {
            report(name, Math.abs(expected - actual) < EPSILON,
                String.format("очікувано=%.6f, отримано=%.6f", expected, actual));
        }

        /** Виводить результат тесту. */
        private void report(String name, boolean success, String details) {
            System.out.printf("  %s : %s",
                success ? "[OK]   PASS" : "[FAIL] FAIL", name);
            if (!details.isEmpty()) System.out.printf("  [%s]", details);
            System.out.println();
            if (success) passed++; else failed++;
        }
    }

    // =========================================================================
    // Утиліти
    // =========================================================================

    /** Ім'я файлу колекції за замовчуванням. */
    private static final String COLLECTION_FILE = "ballistic_collection.ser";

    /**
     * Зчитує дійсне число з консолі з повторним запитом при помилці.
     *
     * @param scanner об'єкт {@link Scanner}
     * @param prompt  запрошення
     * @return введене число
     */
    private static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try   { return Double.parseDouble(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  [!] Некоректне число."); }
        }
    }

    /**
     * Зчитує ціле число з консолі з повторним запитом при помилці.
     *
     * @param scanner об'єкт {@link Scanner}
     * @param prompt  запрошення
     * @return введене ціле число
     */
    private static int readInt(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(prompt);
            try   { return Integer.parseInt(scanner.nextLine().trim()); }
            catch (NumberFormatException e) { System.out.println("  [!] Некоректне число."); }
        }
    }

    /**
     * Зчитує символ з консолі (перший символ рядка).
     *
     * @param scanner  об'єкт {@link Scanner}
     * @param prompt   запрошення
     * @param defValue символ за замовчуванням, якщо введено порожній рядок
     * @return введений символ
     */
    private static char readChar(Scanner scanner, String prompt, char defValue) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        return line.isEmpty() ? defValue : line.charAt(0);
    }

    /**
     * Зчитує відповідь «так/ні» з консолі.
     *
     * @param scanner об'єкт {@link Scanner}
     * @param prompt  запрошення
     * @return {@code true} якщо введено 'y', 'Y', 'т', 'Т', '1'
     */
    private static boolean readBool(Scanner scanner, String prompt) {
        System.out.print(prompt + " [y/n]: ");
        String line = scanner.nextLine().trim().toLowerCase();
        return line.equals("y") || line.equals("т") || line.equals("1");
    }

    /**
     * Зчитує рядок з консолі.
     *
     * @param scanner   об'єкт {@link Scanner}
     * @param prompt    запрошення
     * @param defValue  значення за замовчуванням
     * @return введений рядок або {@code defValue}
     */
    private static String readString(Scanner scanner, String prompt, String defValue) {
        System.out.print(prompt + " [Enter = \"" + defValue + "\"]: ");
        String line = scanner.nextLine().trim();
        return line.isEmpty() ? defValue : line;
    }

    /**
     * Інтерактивно формує {@link TableConfig} з консолі.
     *
     * @param scanner об'єкт {@link Scanner}
     * @return налаштований {@link TableConfig}
     */
    private static TableConfig readTableConfig(Scanner scanner) {
        System.out.println("\n  +--- Налаштування таблиці ---+");
        int  w     = readInt(scanner,    "  > Ширина стовпця (4..30)    : ");
        char sep   = readChar(scanner,   "  > Символ-роздільник         : ", '|');
        boolean sm = readBool(scanner,   "  > Показувати метричну (м)  ");
        boolean si = readBool(scanner,   "  > Показувати англійську (фут)");
        boolean sd = readBool(scanner,   "  > Показувати км/милі        ");
        int  mr    = readInt(scanner,    "  > Макс. рядків (0=всі)      : ");
        String t   = readString(scanner, "  > Заголовок таблиці         ", "Результати");
        TableConfig cfg = new TableConfig(w, sep, sm, si, mr, t, sd);
        System.out.printf("  [OK] Конфігурація: %s%n", cfg);
        return cfg;
    }

    /**
     * Вибирає фабрику за номером, передаючи конфігурацію де потрібно.
     *
     * @param choice номер (1..6)
     * @param config конфігурація таблиці
     * @return відповідна {@link DisplayFactory}
     */
    private static DisplayFactory chooseFactory(int choice, TableConfig config) {
        switch (choice) {
            case 2:  return new DetailedDisplayFactory();
            case 3:  return new CompactDisplayFactory();
            case 4:  return new SimpleTableDisplayFactory(config);
            case 5:  return new BorderedTableDisplayFactory(config);
            case 6:  return new CsvTableDisplayFactory(config);
            default: return new TextDisplayFactory();
        }
    }

    // =========================================================================
    // main
    // =========================================================================

    /**
     * Точка входу: тести → інтерактивне меню.
     *
     * <p>Пункти меню:</p>
     * <ol>
     *   <li>Ввести параметри та обчислити.</li>
     *   <li>Вибрати формат відображення (Factory Method).</li>
     *   <li>Налаштувати параметри таблиці ({@link TableConfig}).</li>
     *   <li>Показати останній результат.</li>
     *   <li>Показати всі результати (з overloading).</li>
     *   <li>Демо поліморфізму (dynamic dispatch).</li>
     *   <li>Зберегти колекцію.</li>
     *   <li>Завантажити колекцію.</li>
     *   <li>Очистити колекцію.</li>
     *   <li>Вихід.</li>
     * </ol>
     *
     * @param args аргументи командного рядка (не використовуються)
     * @throws Exception будь-яка непередбачена помилка
     */
    public static void main(String[] args) throws Exception {

        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        System.setErr(new PrintStream(System.err, true, "UTF-8"));

        // --- Тести ---
        boolean allPassed = new BallisticTest().runAll();
        if (!allPassed) System.err.println("\n[!] Деякі тести провалені.");

        // --- Меню ---
        Scanner scanner   = new Scanner(System.in);
        ResultCollection collection = new ResultCollection();
        TableConfig tableConfig     = new TableConfig();
        DisplayFactory factory      = new TextDisplayFactory();
        BallisticData lastData      = null;

        System.out.println("\n+============================================================+");
        System.out.println("|  Балістичний калькулятор — Factory Method + Polymorphism   |");
        System.out.println("+============================================================+");

        boolean running = true;
        while (running) {
            System.out.println("\n+--- Меню -------------------------------------------+");
            System.out.println("  1. Ввести параметри та обчислити");
            System.out.println("  2. Вибрати формат відображення (Factory Method)");
            System.out.println("  3. Налаштувати параметри таблиці (TableConfig)");
            System.out.println("  4. Показати останній результат");
            System.out.println("  5. Показати всі результати колекції");
            System.out.println("  6. Показати всі — обмежити кількість рядків (overloading)");
            System.out.println("  7. Демонстрація поліморфізму (dynamic dispatch)");
            System.out.println("  8. Зберегти колекцію у файл");
            System.out.println("  9. Завантажити колекцію з файлу");
            System.out.println(" 10. Очистити колекцію");
            System.out.println("  0. Вихід");
            System.out.printf("+--- Колекція: %d записів | Фабрика: %-28s +%n",
                collection.size(), factory.getClass().getSimpleName());

            int choice = readInt(scanner, "> Виберіть пункт: ");

            switch (choice) {

                case 1: {
                    try {
                        double v = readDouble(scanner, "> v0 (м/с): ");
                        double a = readDouble(scanner, "> α (°, 0–90): ");
                        BallisticData d = new BallisticData(v, a);
                        new BallisticSolver(d).solve();
                        collection.add(d);
                        lastData = d;
                        System.out.println("\n" + new BallisticSolver(d).getReport());
                        System.out.printf("[OK] Додано до колекції (всього: %d)%n", collection.size());
                    } catch (IllegalArgumentException e) {
                        System.out.println("  [!] " + e.getMessage());
                    }
                    break;
                }

                case 2: {
                    System.out.println("\n  Оберіть фабрику:");
                    System.out.println("    1. TextDisplayFactory");
                    System.out.println("    2. DetailedDisplayFactory");
                    System.out.println("    3. CompactDisplayFactory");
                    System.out.println("    4. SimpleTableDisplayFactory   (використовує TableConfig)");
                    System.out.println("    5. BorderedTableDisplayFactory (використовує TableConfig)");
                    System.out.println("    6. CsvTableDisplayFactory      (використовує TableConfig)");
                    int fmt = readInt(scanner, "  > Фабрика: ");
                    factory = chooseFactory(fmt, tableConfig);
                    System.out.printf("[OK] Обрано: %s%n", factory.getClass().getSimpleName());
                    break;
                }

                case 3: {
                    tableConfig = readTableConfig(scanner);
                    // Якщо поточна фабрика табличного типу — оновлюємо її
                    if (factory instanceof SimpleTableDisplayFactory
                     || factory instanceof BorderedTableDisplayFactory
                     || factory instanceof CsvTableDisplayFactory) {
                        factory = chooseFactory(
                            factory instanceof BorderedTableDisplayFactory ? 5
                          : factory instanceof CsvTableDisplayFactory       ? 6 : 4,
                            tableConfig);
                        System.out.printf("[OK] Фабрику оновлено з новою конфігурацією: %s%n",
                            factory.getClass().getSimpleName());
                    }
                    break;
                }

                case 4: {
                    if (lastData == null) {
                        System.out.println("  [!] Немає результатів.");
                    } else {
                        factory.createDisplay(lastData).displayFull();
                    }
                    break;
                }

                case 5: {
                    if (collection.size() == 0) {
                        System.out.println("  [!] Колекція порожня.");
                    } else {
                        BallisticData ref = lastData != null
                            ? lastData : collection.getAll().get(0);
                        factory.createDisplay(ref).displayTable(collection.getAll());
                    }
                    break;
                }

                case 6: {
                    // Демонстрація overloading: displayTable(List, int)
                    if (collection.size() == 0) {
                        System.out.println("  [!] Колекція порожня.");
                        break;
                    }
                    int lim = readInt(scanner, "> Ліміт рядків: ");
                    BallisticData ref = lastData != null ? lastData : collection.getAll().get(0);
                    Displayable d = factory.createDisplay(ref);
                    System.out.println("\n[Overloading] displayTable(List, int) — ліміт=" + lim);
                    if (d instanceof ConfigurableDisplay) {
                        ((ConfigurableDisplay) d).displayTable(collection.getAll(), lim);
                    } else {
                        // Fallback: TextDisplay не ConfigurableDisplay
                        d.displayTable(collection.getAll());
                        System.out.println("  [Примітка] Обраний формат не підтримує ліміт рядків.");
                    }
                    break;
                }

                case 7: {
                    // Демонстрація поліморфізму (dynamic method dispatch)
                    if (collection.size() == 0) {
                        System.out.println("  [!] Колекція порожня. Додайте хоча б один запис.");
                        break;
                    }
                    BallisticData ref = lastData != null ? lastData : collection.getAll().get(0);
                    System.out.println("\n+======== Демонстрація поліморфізму (Dynamic Dispatch) ========+");
                    System.out.println("  Виклик displayShort() через Displayable[] — пізнє зв'язування:");
                    System.out.println("+--------------------------------------------------------------+");

                    // Масив різних реалізацій — поліморфний виклик
                    Displayable[] displays = {
                        new TextDisplay(ref),
                        new SimpleTableDisplay(ref, tableConfig),
                        new BorderedTableDisplay(ref, tableConfig),
                        new DetailedTextDisplay(ref),
                        new CompactDisplay(ref),
                        new CsvTableDisplay(ref, tableConfig)
                    };

                    for (Displayable disp : displays) {
                        System.out.printf("  %-35s → ", disp.getClass().getSimpleName());
                        disp.displayShort(); // динамічний dispatch
                    }

                    System.out.println("+--------------------------------------------------------------+");
                    System.out.println("  displayTable(list) через Displayable[] — пізнє зв'язування:");
                    System.out.println("+--------------------------------------------------------------+");
                    for (Displayable disp : displays) {
                        System.out.printf("%n  === %s ===%n", disp.getClass().getSimpleName());
                        disp.displayTable(collection.getAll());
                    }
                    System.out.println("+==============================================================+");
                    break;
                }

                case 8: {
                    try {
                        collection.saveToFile(COLLECTION_FILE);
                        System.out.printf("[OK] Збережено \"%s\" (%d записів)%n",
                            COLLECTION_FILE, collection.size());
                    } catch (IOException e) {
                        System.out.println("  [!] " + e.getMessage());
                    }
                    break;
                }

                case 9: {
                    try {
                        collection.loadFromFile(COLLECTION_FILE);
                        System.out.printf("[OK] Завантажено \"%s\" (%d записів)%n",
                            COLLECTION_FILE, collection.size());
                        if (collection.size() > 0)
                            lastData = collection.getAll().get(collection.size() - 1);
                    } catch (FileNotFoundException e) {
                        System.out.printf("  [!] Файл \"%s\" не знайдено.%n", COLLECTION_FILE);
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("  [!] " + e.getMessage());
                    }
                    break;
                }

                case 10: {
                    collection.clear();
                    lastData = null;
                    System.out.println("[OK] Колекцію очищено.");
                    break;
                }

                case 0: {
                    running = false;
                    System.out.println("\nДемонстрацію завершено. Ракета полетіла.");
                    break;
                }

                default:
                    System.out.println("  [!] Невідомий пункт меню.");
            }
        }

        scanner.close();
    }
}