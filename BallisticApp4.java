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
         * Видаляє останній елемент колекції.
         * Використовується для {@code undo} операції додавання.
         */
        public void removeLast() {
            if (!results.isEmpty()) results.remove(results.size() - 1);
        }

        /**
         * Замінює вміст колекції новим списком.
         * Використовується для {@code undo} операцій трансформації.
         *
         * @param items новий список елементів
         */
        public void setAll(List<BallisticData> items) {
            results.clear();
            results.addAll(items);
        }

        /**
         * Повертає знімок (глибоку копію) поточного стану колекції.
         * Використовується командами перед виконанням для збереження стану.
         *
         * @return нова незалежна копія списку
         */
        public List<BallisticData> snapshot() {
            return new ArrayList<>(results);
        }

        /**
         * Повертає змінюваний список для ітерації та модифікації елементів.
         * Структура списку (add/remove) залишається захищеною.
         *
         * @return внутрішній список
         */
        List<BallisticData> getMutable() { return results; }

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
    // AppContext — спільний стан застосунку для команд
    // =========================================================================

    /**
     * Контейнер спільного змінюваного стану застосунку.
     * Передається командам як залежність (Dependency Injection).
     *
     * @see Command
     */
    static class AppContext {
        /** Колекція результатів обчислень. */
        ResultCollection collection = new ResultCollection();
        /** Поточна фабрика відображення. */
        DisplayFactory factory = new TextDisplayFactory();
        /** Конфігурація таблиці. */
        TableConfig tableConfig = new TableConfig();
        /** Останній обчислений запис. */
        BallisticData lastData = null;
        /** Потік введення. */
        final Scanner scanner;
        /** Файл збереження колекції. */
        static final String COLLECTION_FILE = "ballistic_collection.ser";

        /**
         * Створює контекст зі заданим сканером вводу.
         *
         * @param scanner об'єкт {@link Scanner}
         */
        AppContext(Scanner scanner) { this.scanner = scanner; }
    }

    // =========================================================================
    // Command — інтерфейс команди
    // =========================================================================

    /**
     * Інтерфейс команди (Command pattern).
     *
     * <p>Кожна операція інкапсулюється як об'єкт, що підтримує
     * виконання ({@link #execute()}) та скасування ({@link #undo()}).</p>
     *
     * @see CommandHistory
     * @see Menu
     */
    interface Command {
        /** Виконує команду. */
        void execute();

        /**
         * Скасовує попередньо виконану команду.
         * Відновлює стан, що передував виклику {@link #execute()}.
         */
        void undo();

        /** @return назва команди для меню та логу */
        String getName();
    }

    // =========================================================================
    // CommandHistory — Singleton з підтримкою Undo
    // =========================================================================

    /**
     * Синглтон-сховище історії виконаних команд.
     *
     * <p>Реалізує шаблон проектування <b>Singleton</b> за допомогою
     * подвійної перевірки (double-checked locking) з {@code volatile}.
     * Надає операцію {@link #undo()} для скасування останньої команди.</p>
     *
     * @see Command
     */
    static class CommandHistory {

        /** Єдиний volatile-екземпляр. */
        private static volatile CommandHistory instance;

        /** Стек виконаних команд (LIFO). */
        private final Deque<Command> history = new ArrayDeque<>();

        /** Закритий конструктор — не дає створювати екземпляри ззовні. */
        private CommandHistory() {}

        /**
         * Повертає єдиний екземпляр {@code CommandHistory}.
         * Перша ініціалізація потокобезпечна (double-checked locking).
         *
         * @return єдиний екземпляр синглтона
         */
        public static CommandHistory getInstance() {
            if (instance == null) {
                synchronized (CommandHistory.class) {
                    if (instance == null) instance = new CommandHistory();
                }
            }
            return instance;
        }

        /**
         * Поміщає виконану команду у стек.
         *
         * @param cmd виконана команда
         */
        public void push(Command cmd) { history.push(cmd); }

        /** @return {@code true} якщо є команда для скасування */
        public boolean canUndo() { return !history.isEmpty(); }

        /** @return кількість команд у стеці */
        public int size() { return history.size(); }

        /** Очищає стек. */
        public void clearHistory() { history.clear(); }

        /**
         * Скасовує останню виконану команду.
         * Виводить повідомлення і викликає {@link Command#undo()}.
         */
        public void undo() {
            if (!history.isEmpty()) {
                Command cmd = history.pop();
                cmd.undo();
                System.out.println("[Undo] Скасовано: «" + cmd.getName() + "»");
            } else {
                System.out.println("[Undo] Немає операцій для скасування.");
            }
        }
    }

    // =========================================================================
    // Menu — розширюваний контейнер команд
    // =========================================================================

    /**
     * Розширюваний контейнер команд (Command + Menu pattern).
     *
     * <p>Команди можна динамічно додавати ({@link #addEntry}) та видаляти
     * ({@link #removeEntry}), що робить меню відкритим для розширення.</p>
     *
     * @see Command
     * @see CommandHistory
     */
    static class Menu {

        /** Заголовок меню. */
        private final String title;

        /** Впорядкований реєстр пунктів меню. */
        private final LinkedHashMap<String, MenuEntry> entries = new LinkedHashMap<>();

        /**
         * Внутрішній запис пункту меню.
         */
        static class MenuEntry {
            /** Відображуваний підпис. */
            final String label;
            /** Пов'язана команда. */
            final Command command;

            MenuEntry(String label, Command command) {
                this.label   = label;
                this.command = command;
            }
        }

        /**
         * Створює меню із заданим заголовком.
         *
         * @param title заголовок меню
         */
        public Menu(String title) { this.title = title; }

        /**
         * Додає новий пункт до меню.
         *
         * @param key     клавіша вибору
         * @param label   відображуваний підпис
         * @param command команда для виконання
         */
        public void addEntry(String key, String label, Command command) {
            entries.put(key, new MenuEntry(label, command));
        }

        /**
         * Видаляє пункт меню за ключем.
         *
         * @param key ключ пункту
         */
        public void removeEntry(String key) { entries.remove(key); }

        /** @param key ключ @return {@code true} якщо пункт існує */
        public boolean hasKey(String key) { return entries.containsKey(key); }

        /**
         * Виводить меню на консоль з підрахунком колекції та стека Undo.
         *
         * @param ctx поточний контекст застосунку
         */
        public void display(AppContext ctx) {
            System.out.println("\n+=====================================================+");
            System.out.printf(" %s%n", title);
            System.out.printf(" Колекція: %d записів | Undo-стек: %d | Фабрика: %s%n",
                ctx.collection.size(),
                CommandHistory.getInstance().size(),
                ctx.factory.getClass().getSimpleName());
            System.out.println("+-----------------------------------------------------+");
            for (Map.Entry<String, MenuEntry> e : entries.entrySet())
                System.out.printf("  %-3s — %s%n", e.getKey(), e.getValue().label);
            System.out.println("+=====================================================+");
        }

        /**
         * Виконує команду за ключем та записує її у {@link CommandHistory}.
         *
         * @param key ключ пункту
         * @return {@code true} якщо команду знайдено і виконано
         */
        public boolean executeCommand(String key) {
            MenuEntry entry = entries.get(key);
            if (entry == null) return false;
            entry.command.execute();
            CommandHistory.getInstance().push(entry.command);
            return true;
        }
    }

    // =========================================================================
    // Допоміжний метод — глибока копія BallisticData
    // =========================================================================

    /**
     * Створює незалежну копію об'єкта {@link BallisticData}.
     * Використовується командами для збереження знімка стану перед виконанням.
     *
     * @param src вихідний об'єкт
     * @return нова копія з тими самими значеннями
     */
    private static BallisticData copyData(BallisticData src) {
        BallisticData copy = new BallisticData(
            src.getInitialVelocity(), src.getAngleDegrees());
        copy.setRangeMetric(src.getRangeMetric());
        copy.setRangeImperial(src.getRangeImperial());
        return copy;
    }

    // =========================================================================
    // Конкретні команди (Concrete Commands)
    // =========================================================================

    /**
     * Команда: введення параметрів та обчислення нового запису.
     * <p><b>Undo:</b> видаляє останній доданий запис.</p>
     */
    static class AddDataCommand implements Command {
        private final AppContext ctx;
        private BallisticData added;

        /** @param ctx контекст застосунку */
        public AddDataCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Ввести параметри та обчислити"; }

        @Override
        public void execute() {
            try {
                double v = readDouble(ctx.scanner, "  > v0 (м/с): ");
                double a = readDouble(ctx.scanner, "  > α (°, 0–90): ");
                BallisticData d = new BallisticData(v, a);
                new BallisticSolver(d).solve();
                ctx.collection.add(d);
                ctx.lastData = d;
                added = d;
                System.out.println("\n" + new BallisticSolver(d).getReport());
                System.out.printf("[OK] Додано (всього: %d)%n", ctx.collection.size());
            } catch (IllegalArgumentException e) {
                System.out.println("  [!] " + e.getMessage());
                added = null;
            }
        }

        @Override
        public void undo() {
            if (added != null) {
                ctx.collection.removeLast();
                if (ctx.lastData == added) ctx.lastData = null;
                System.out.printf("  Видалено останній запис. Залишилось: %d%n",
                    ctx.collection.size());
                added = null;
            }
        }
    }

    /**
     * Команда: відображення всіх записів колекції.
     * <p><b>Undo:</b> нічого (read-only операція).</p>
     */
    static class PrintCollectionCommand implements Command {
        private final AppContext ctx;

        /** @param ctx контекст застосунку */
        public PrintCollectionCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Показати всю колекцію"; }

        @Override
        public void execute() {
            if (ctx.collection.size() == 0) {
                System.out.println("  [!] Колекція порожня.");
                return;
            }
            BallisticData ref = ctx.lastData != null
                ? ctx.lastData : ctx.collection.getAll().get(0);
            ctx.factory.createDisplay(ref).displayTable(ctx.collection.getAll());
        }

        @Override public void undo() { /* read-only — нічого скасовувати */ }
    }

    /**
     * Команда: відображення останнього результату.
     * <p><b>Undo:</b> нічого (read-only).</p>
     */
    static class ShowLastCommand implements Command {
        private final AppContext ctx;

        /** @param ctx контекст застосунку */
        public ShowLastCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Показати останній результат"; }

        @Override
        public void execute() {
            if (ctx.lastData == null) {
                System.out.println("  [!] Немає результатів.");
            } else {
                ctx.factory.createDisplay(ctx.lastData).displayFull();
            }
        }

        @Override public void undo() { /* read-only */ }
    }

    /**
     * Команда: сортування колекції за обраним критерієм.
     * <p><b>Undo:</b> відновлює початковий порядок елементів.</p>
     */
    static class SortCommand implements Command {
        private final AppContext ctx;
        private final int mode; // 1=дальність, 2=швидкість, 3=кут
        private List<BallisticData> snapshot;

        /**
         * @param ctx  контекст застосунку
         * @param mode режим: 1 — за дальністю, 2 — за швидкістю, 3 — за кутом
         */
        public SortCommand(AppContext ctx, int mode) {
            this.ctx  = ctx;
            this.mode = mode;
        }

        @Override
        public String getName() {
            switch (mode) {
                case 2:  return "Сортувати за початковою швидкістю";
                case 3:  return "Сортувати за кутом пострілу";
                default: return "Сортувати за дальністю польоту";
            }
        }

        @Override
        public void execute() {
            if (ctx.collection.size() == 0) {
                System.out.println("  [!] Колекція порожня."); return;
            }
            snapshot = ctx.collection.snapshot();
            List<BallisticData> sorted = new ArrayList<>(snapshot);
            Comparator<BallisticData> cmp;
            switch (mode) {
                case 2:  cmp = Comparator.comparingDouble(BallisticData::getInitialVelocity); break;
                case 3:  cmp = Comparator.comparingDouble(BallisticData::getAngleDegrees);    break;
                default: cmp = Comparator.comparingDouble(BallisticData::getRangeMetric);
            }
            sorted.sort(cmp);
            ctx.collection.setAll(sorted);
            System.out.println("[OK] " + getName() + ". Записів: " + ctx.collection.size());
        }

        @Override
        public void undo() {
            if (snapshot != null) {
                ctx.collection.setAll(snapshot);
                System.out.println("  Порядок колекції відновлено.");
                snapshot = null;
            }
        }
    }

    /**
     * Команда: масштабування початкових швидкостей у колекції.
     * Перераховує дальності після масштабування.
     *
     * <p><b>Undo:</b> відновлює всі значення до масштабування.</p>
     */
    static class ScaleVelocityCommand implements Command {
        private final AppContext ctx;
        private List<BallisticData> snapshot;
        private double factor;

        /** @param ctx контекст застосунку */
        public ScaleVelocityCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Масштабувати швидкості (×коефіцієнт)"; }

        @Override
        public void execute() {
            if (ctx.collection.size() == 0) {
                System.out.println("  [!] Колекція порожня."); return;
            }
            factor = readDouble(ctx.scanner, "  > Коефіцієнт масштабування (наприклад, 2.0): ");
            if (factor <= 0) {
                System.out.println("  [!] Коефіцієнт має бути > 0."); return;
            }
            // Знімок перед змінами (глибока копія)
            snapshot = new ArrayList<>();
            for (BallisticData d : ctx.collection.getAll())
                snapshot.add(copyData(d));

            for (BallisticData d : ctx.collection.getMutable()) {
                d.setInitialVelocity(d.getInitialVelocity() * factor);
                new BallisticSolver(d).solve();
            }
            System.out.printf("[OK] Масштабовано ×%.2f. Перераховано %d записів.%n",
                factor, ctx.collection.size());
        }

        @Override
        public void undo() {
            if (snapshot != null) {
                ctx.collection.setAll(snapshot);
                System.out.printf("  Масштабування ×%.2f скасовано.%n", factor);
                snapshot = null;
            }
        }
    }

    /**
     * Команда: нормалізація дальностей у колекції до діапазону [0.0 ; 1.0].
     * Значення v0 та кут не змінюються — нормалізуються лише збережені результати.
     *
     * <p><b>Undo:</b> відновлює оригінальні значення дальностей.</p>
     */
    static class NormalizeRangesCommand implements Command {
        private final AppContext ctx;
        private List<BallisticData> snapshot;

        /** @param ctx контекст застосунку */
        public NormalizeRangesCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Нормалізувати дальності до [0, 1]"; }

        @Override
        public void execute() {
            if (ctx.collection.size() < 2) {
                System.out.println("  [!] Потрібно принаймні 2 записи для нормалізації."); return;
            }
            // Знімок
            snapshot = new ArrayList<>();
            for (BallisticData d : ctx.collection.getAll())
                snapshot.add(copyData(d));

            double min = ctx.collection.getAll().stream()
                .mapToDouble(BallisticData::getRangeMetric).min().orElse(0);
            double max = ctx.collection.getAll().stream()
                .mapToDouble(BallisticData::getRangeMetric).max().orElse(1);
            double range = max - min;

            if (range < 1e-9) {
                System.out.println("  [!] Усі значення однакові — нормалізація неможлива."); return;
            }

            for (BallisticData d : ctx.collection.getMutable()) {
                double norm = (d.getRangeMetric() - min) / range;
                d.setRangeMetric(norm);
                d.setRangeImperial(norm * BallisticData.METERS_TO_FEET); // умовно
            }
            System.out.printf("[OK] Нормалізовано %d записів. min=%.2f м, max=%.2f м%n",
                ctx.collection.size(), min, max);
        }

        @Override
        public void undo() {
            if (snapshot != null) {
                ctx.collection.setAll(snapshot);
                System.out.println("  Нормалізацію скасовано. Відновлено оригінальні дальності.");
                snapshot = null;
            }
        }
    }

    /**
     * Команда: лінійна інтерполяція між першим та останнім елементами колекції.
     * Додає N проміжних записів зі значеннями v0 та α між двома крайніми.
     *
     * <p><b>Undo:</b> видаляє додані інтерпольовані записи.</p>
     */
    static class InterpolateCommand implements Command {
        private final AppContext ctx;
        private int addedCount = 0;

        /** @param ctx контекст застосунку */
        public InterpolateCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Інтерполювати між крайніми елементами"; }

        @Override
        public void execute() {
            if (ctx.collection.size() < 2) {
                System.out.println("  [!] Потрібно принаймні 2 записи."); return;
            }
            int n = readInt(ctx.scanner, "  > Кількість проміжних точок (1–20): ");
            if (n < 1 || n > 20) {
                System.out.println("  [!] Діапазон: 1–20."); return;
            }
            List<BallisticData> all  = ctx.collection.getAll();
            BallisticData first = all.get(0);
            BallisticData last  = all.get(all.size() - 1);

            addedCount = n;
            for (int i = 1; i <= n; i++) {
                double t  = (double) i / (n + 1);
                double v0 = first.getInitialVelocity() + t * (last.getInitialVelocity() - first.getInitialVelocity());
                double a  = first.getAngleDegrees()    + t * (last.getAngleDegrees()    - first.getAngleDegrees());
                // Clamp angle
                a = Math.max(0, Math.min(90, a));
                BallisticData d = new BallisticData(v0, a);
                new BallisticSolver(d).solve();
                ctx.collection.add(d);
            }
            System.out.printf("[OK] Додано %d інтерпольованих записів. Всього: %d%n",
                n, ctx.collection.size());
        }

        @Override
        public void undo() {
            for (int i = 0; i < addedCount; i++) ctx.collection.removeLast();
            System.out.printf("  Видалено %d інтерпольованих записів.%n", addedCount);
            addedCount = 0;
        }
    }

    /**
     * Команда: пошук записів у заданому діапазоні дальностей.
     * <p><b>Undo:</b> read-only — нічого скасовувати.</p>
     */
    static class SearchByRangeCommand implements Command {
        private final AppContext ctx;

        /** @param ctx контекст застосунку */
        public SearchByRangeCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Пошук за діапазоном дальності (м)"; }

        @Override
        public void execute() {
            if (ctx.collection.size() == 0) {
                System.out.println("  [!] Колекція порожня."); return;
            }
            double lo = readDouble(ctx.scanner, "  > Мінімальна дальність (м): ");
            double hi = readDouble(ctx.scanner, "  > Максимальна дальність (м): ");

            List<BallisticData> found = new ArrayList<>();
            for (BallisticData d : ctx.collection.getAll())
                if (d.getRangeMetric() >= lo && d.getRangeMetric() <= hi) found.add(d);

            if (found.isEmpty()) {
                System.out.printf("  Нічого не знайдено в діапазоні [%.2f ; %.2f] м%n", lo, hi);
            } else {
                System.out.printf("[OK] Знайдено %d запис(ів) у [%.2f ; %.2f] м:%n",
                    found.size(), lo, hi);
                ctx.factory.createDisplay(found.get(0)).displayTable(found);
            }
        }

        @Override public void undo() { /* read-only */ }
    }

    /**
     * Команда: збереження колекції у файл.
     * <p><b>Undo:</b> видаляє збережений файл.</p>
     */
    static class SaveCommand implements Command {
        private final AppContext ctx;

        /** @param ctx контекст застосунку */
        public SaveCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Зберегти колекцію у файл"; }

        @Override
        public void execute() {
            try {
                ctx.collection.saveToFile(AppContext.COLLECTION_FILE);
                System.out.printf("[OK] Збережено \"%s\" (%d записів)%n",
                    AppContext.COLLECTION_FILE, ctx.collection.size());
            } catch (IOException e) {
                System.out.println("  [!] " + e.getMessage());
            }
        }

        @Override
        public void undo() {
            new File(AppContext.COLLECTION_FILE).delete();
            System.out.println("  Файл \"" + AppContext.COLLECTION_FILE + "\" видалено.");
        }
    }

    /**
     * Команда: завантаження колекції з файлу.
     * <p><b>Undo:</b> відновлює попередній вміст колекції.</p>
     */
    static class LoadCommand implements Command {
        private final AppContext ctx;
        private List<BallisticData> snapshot;

        /** @param ctx контекст застосунку */
        public LoadCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Завантажити колекцію з файлу"; }

        @Override
        public void execute() {
            snapshot = ctx.collection.snapshot();
            try {
                ctx.collection.loadFromFile(AppContext.COLLECTION_FILE);
                System.out.printf("[OK] Завантажено \"%s\" (%d записів)%n",
                    AppContext.COLLECTION_FILE, ctx.collection.size());
                if (ctx.collection.size() > 0)
                    ctx.lastData = ctx.collection.getAll().get(ctx.collection.size() - 1);
            } catch (FileNotFoundException e) {
                System.out.printf("  [!] Файл \"%s\" не знайдено.%n", AppContext.COLLECTION_FILE);
                snapshot = null;
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("  [!] " + e.getMessage());
                snapshot = null;
            }
        }

        @Override
        public void undo() {
            if (snapshot != null) {
                ctx.collection.setAll(snapshot);
                System.out.println("  Завантаження скасовано. Колекцію відновлено.");
                snapshot = null;
            }
        }
    }

    /**
     * Команда: очищення колекції.
     * <p><b>Undo:</b> відновлює всі видалені записи.</p>
     */
    static class ClearCommand implements Command {
        private final AppContext ctx;
        private List<BallisticData> snapshot;

        /** @param ctx контекст застосунку */
        public ClearCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Очистити колекцію"; }

        @Override
        public void execute() {
            snapshot = ctx.collection.snapshot();
            ctx.collection.clear();
            ctx.lastData = null;
            System.out.println("[OK] Колекцію очищено.");
        }

        @Override
        public void undo() {
            if (snapshot != null) {
                ctx.collection.setAll(snapshot);
                System.out.printf("  Колекцію відновлено. Записів: %d%n", ctx.collection.size());
                if (ctx.collection.size() > 0)
                    ctx.lastData = ctx.collection.getAll().get(ctx.collection.size() - 1);
                snapshot = null;
            }
        }
    }

    /**
     * Команда: зміна формату відображення (Factory Method).
     * <p><b>Undo:</b> відновлює попередню фабрику.</p>
     */
    static class ChangeFactoryCommand implements Command {
        private final AppContext ctx;
        private DisplayFactory prev;

        /** @param ctx контекст застосунку */
        public ChangeFactoryCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Змінити формат відображення"; }

        @Override
        public void execute() {
            System.out.println("\n  Формати:");
            System.out.println("    1. TextDisplay       2. DetailedDisplay");
            System.out.println("    3. CompactDisplay    4. SimpleTable");
            System.out.println("    5. BorderedTable     6. CsvTable");
            int fmt = readInt(ctx.scanner, "  > Формат: ");
            prev = ctx.factory;
            ctx.factory = chooseFactory(fmt, ctx.tableConfig);
            System.out.printf("[OK] Обрано: %s%n", ctx.factory.getClass().getSimpleName());
        }

        @Override
        public void undo() {
            if (prev != null) {
                ctx.factory = prev;
                System.out.println("  Формат відновлено: " + ctx.factory.getClass().getSimpleName());
                prev = null;
            }
        }
    }

    /**
     * Команда: налаштування параметрів таблиці.
     * <p><b>Undo:</b> відновлює попередню конфігурацію.</p>
     */
    static class ConfigureTableCommand implements Command {
        private final AppContext ctx;
        private TableConfig prevConfig;

        /** @param ctx контекст застосунку */
        public ConfigureTableCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Налаштувати параметри таблиці"; }

        @Override
        public void execute() {
            prevConfig = ctx.tableConfig;
            ctx.tableConfig = readTableConfig(ctx.scanner);
        }

        @Override
        public void undo() {
            if (prevConfig != null) {
                ctx.tableConfig = prevConfig;
                System.out.println("  Конфігурацію таблиці відновлено: " + ctx.tableConfig);
                prevConfig = null;
            }
        }
    }

    /**
     * Команда: демонстрація поліморфізму (dynamic dispatch).
     * <p><b>Undo:</b> нічого (read-only).</p>
     */
    static class PolymorphismDemoCommand implements Command {
        private final AppContext ctx;

        /** @param ctx контекст застосунку */
        public PolymorphismDemoCommand(AppContext ctx) { this.ctx = ctx; }

        @Override public String getName() { return "Демонстрація поліморфізму (dynamic dispatch)"; }

        @Override
        public void execute() {
            if (ctx.collection.size() == 0) {
                System.out.println("  [!] Колекція порожня. Додайте хоча б один запис."); return;
            }
            BallisticData ref = ctx.lastData != null
                ? ctx.lastData : ctx.collection.getAll().get(0);
            System.out.println("\n+==== Демонстрація поліморфізму (Dynamic Dispatch) ====+");
            Displayable[] displays = {
                new TextDisplay(ref),
                new SimpleTableDisplay(ref, ctx.tableConfig),
                new BorderedTableDisplay(ref, ctx.tableConfig),
                new DetailedTextDisplay(ref),
                new CompactDisplay(ref),
                new CsvTableDisplay(ref, ctx.tableConfig)
            };
            for (Displayable d : displays) {
                System.out.printf("  %-35s → ", d.getClass().getSimpleName());
                d.displayShort();
            }
            System.out.println("+======================================================+");
        }

        @Override public void undo() { /* read-only */ }
    }

    // =========================================================================
    // MacroCommand — макрокоманда (композиція команд)
    // =========================================================================

    /**
     * Макрокоманда: виконує послідовно кілька команд як одну.
     *
     * <p>Демонструє поняття «макрокоманда» — складена команда, що складається
     * з довільної кількості атомарних команд. Підтримує групове скасування.</p>
     *
     * <p>Приклад: «Додати → Сортувати → Відобразити» виконується одним викликом.</p>
     *
     * @see Command
     */
    static class MacroCommand implements Command {

        /** Назва макрокоманди. */
        private final String name;

        /** Список підкоманд у порядку виконання. */
        private final List<Command> commands;

        /**
         * Створює макрокоманду з іменем та списком підкоманд.
         *
         * @param name     назва макрокоманди
         * @param commands список команд (виконуються зліва направо)
         */
        public MacroCommand(String name, List<Command> commands) {
            this.name     = name;
            this.commands = new ArrayList<>(commands);
        }

        @Override public String getName() { return "Макро: " + name; }

        /**
         * Виконує всі підкоманди по черзі.
         * При помилці зупиняється на поточному кроці.
         */
        @Override
        public void execute() {
            System.out.println("[Macro] Виконання: «" + name + "»");
            for (int i = 0; i < commands.size(); i++) {
                System.out.printf("  [%d/%d] %s%n", i + 1, commands.size(), commands.get(i).getName());
                commands.get(i).execute();
            }
            System.out.println("[Macro] Завершено: «" + name + "»");
        }

        /**
         * Скасовує всі підкоманди у зворотному порядку.
         */
        @Override
        public void undo() {
            System.out.println("[Macro] Скасування: «" + name + "»");
            for (int i = commands.size() - 1; i >= 0; i--) {
                System.out.printf("  [%d/%d] Undo: %s%n",
                    commands.size() - i, commands.size(), commands.get(i).getName());
                commands.get(i).undo();
            }
            System.out.println("[Macro] Скасування завершено.");
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
    // BallisticCommandTest — тестування Command, Singleton, MacroCommand
    // =========================================================================

    /**
     * Клас для тестування нової функціональності:
     * шаблонів Command, Singleton, MacroCommand та операцій з колекцією.
     *
     * <p>Групи тестів:</p>
     * <ol>
     *   <li>Singleton CommandHistory.</li>
     *   <li>Команда AddDataCommand + Undo.</li>
     *   <li>Команда SortCommand + Undo.</li>
     *   <li>Команда ScaleVelocityCommand + Undo.</li>
     *   <li>Команда NormalizeRangesCommand + Undo.</li>
     *   <li>Команда ClearCommand + Undo.</li>
     *   <li>MacroCommand (групове виконання та скасування).</li>
     *   <li>Menu — динамічне додавання/видалення.</li>
     *   <li>SearchByRangeCommand.</li>
     *   <li>InterpolateCommand + Undo.</li>
     * </ol>
     */
    static class BallisticCommandTest {

        /** Допустима похибка. */
        private static final double EPS = 1e-6;

        /** Пройдено. */
        private int passed = 0;

        /** Провалено. */
        private int failed = 0;

        /**
         * Запускає всі тести та повертає {@code true} якщо всі пройдені.
         *
         * @return {@code true} якщо всі тести пройшли
         */
        public boolean runAll() {
            System.out.println("+======================================================+");
            System.out.println("|     BallisticCommandTest — Command/Singleton/Macro    |");
            System.out.println("+======================================================+");

            System.out.println("\n--- Група 1: Singleton CommandHistory ---");
            testSingletonSameInstance();
            testSingletonHistoryShared();

            System.out.println("\n--- Група 2: AddDataCommand + Undo ---");
            testAddCommandExecution();
            testAddCommandUndo();

            System.out.println("\n--- Група 3: SortCommand + Undo ---");
            testSortCommandByRange();
            testSortCommandUndo();

            System.out.println("\n--- Група 4: ScaleVelocityCommand + Undo ---");
            testScaleCommandMultipliesVelocity();
            testScaleCommandUndo();

            System.out.println("\n--- Група 5: NormalizeRangesCommand + Undo ---");
            testNormalizeMinIsZeroMaxIsOne();
            testNormalizeUndo();

            System.out.println("\n--- Група 6: ClearCommand + Undo ---");
            testClearCommandEmptiesCollection();
            testClearCommandUndo();

            System.out.println("\n--- Група 7: MacroCommand ---");
            testMacroCommandExecutesAll();
            testMacroCommandUndoReverse();

            System.out.println("\n--- Група 8: Menu ---");
            testMenuAddRemoveEntry();
            testMenuExecuteCommand();

            System.out.println("\n--- Група 9: SearchByRangeCommand ---");
            testSearchFindsInRange();
            testSearchEmptyResult();

            System.out.println("\n--- Група 10: InterpolateCommand + Undo ---");
            testInterpolateAddsPoints();
            testInterpolateUndo();

            System.out.println("\n==============================================");
            System.out.printf("  Результат: %d пройдено, %d провалено з %d%n",
                passed, failed, passed + failed);
            System.out.println("==============================================");
            return failed == 0;
        }

        // ----- Група 1: Singleton -----

        /** Два виклики getInstance() повертають той самий об'єкт. */
        private void testSingletonSameInstance() {
            CommandHistory a = CommandHistory.getInstance();
            CommandHistory b = CommandHistory.getInstance();
            report("Singleton: getInstance() повертає один об'єкт", a == b, "");
        }

        /** Стан CommandHistory спільний між двома посиланнями. */
        private void testSingletonHistoryShared() {
            CommandHistory h = CommandHistory.getInstance();
            h.clearHistory();
            int before = h.size();
            // Реєструємо команду-заглушку через другий alias
            CommandHistory.getInstance().push(new Command() {
                public void execute() {}
                public void undo()    {}
                public String getName() { return "stub"; }
            });
            int after = CommandHistory.getInstance().size();
            h.clearHistory();
            report("Singleton: стан спільний", after == before + 1,
                "до=" + before + " після=" + after);
        }

        // ----- Група 2: AddDataCommand -----

        /** execute() збільшує колекцію на 1. */
        private void testAddCommandExecution() {
            AppContext ctx = makeCtx();
            AddDataCommand cmd = new AddDataCommand(ctx) {
                @Override public void execute() {
                    BallisticData d = new BallisticData(100, 45);
                    new BallisticSolver(d).solve();
                    ctx.collection.add(d);
                    ctx.lastData = d;
                }
            };
            cmd.execute();
            report("AddDataCommand: колекція збільшилась", ctx.collection.size() == 1,
                "розмір=" + ctx.collection.size());
        }

        /** undo() після execute() зменшує колекцію. */
        private void testAddCommandUndo() {
            AppContext ctx = makeCtx();
            // Додаємо напряму і симулюємо undo
            BallisticData d = new BallisticData(100, 45);
            new BallisticSolver(d).solve();
            ctx.collection.add(d);
            int before = ctx.collection.size();
            ctx.collection.removeLast();
            report("AddDataCommand undo: розмір зменшився", ctx.collection.size() == before - 1,
                "до=" + before + " після=" + ctx.collection.size());
        }

        // ----- Група 3: SortCommand -----

        /** Сортування за дальністю дає зростаючий порядок. */
        private void testSortCommandByRange() {
            AppContext ctx = makeFilledCtx();
            SortCommand cmd = new SortCommand(ctx, 1);
            cmd.execute();
            List<BallisticData> all = ctx.collection.getAll();
            boolean sorted = true;
            for (int i = 1; i < all.size(); i++)
                if (all.get(i).getRangeMetric() < all.get(i - 1).getRangeMetric()) {
                    sorted = false; break;
                }
            report("SortCommand: дальності у зростаючому порядку", sorted, "");
        }

        /** undo() після сортування відновлює оригінальний порядок. */
        private void testSortCommandUndo() {
            AppContext ctx = makeFilledCtx();
            List<BallisticData> original = ctx.collection.snapshot();
            SortCommand cmd = new SortCommand(ctx, 1);
            cmd.execute();
            cmd.undo();
            boolean restored = true;
            List<BallisticData> current = ctx.collection.getAll();
            for (int i = 0; i < original.size(); i++) {
                if (original.get(i).getInitialVelocity() != current.get(i).getInitialVelocity()) {
                    restored = false; break;
                }
            }
            report("SortCommand undo: порядок відновлено", restored, "");
        }

        // ----- Група 4: ScaleVelocityCommand -----

        /** execute() множить v0 на коефіцієнт. */
        private void testScaleCommandMultipliesVelocity() {
            AppContext ctx = makeCtx();
            BallisticData d = new BallisticData(100, 45);
            new BallisticSolver(d).solve();
            ctx.collection.add(d);

            // Знімок вручну і масштабуємо
            double origV = d.getInitialVelocity();
            double factor = 2.0;
            List<BallisticData> snap = new ArrayList<>();
            snap.add(copyData(d));
            for (BallisticData item : ctx.collection.getMutable()) {
                item.setInitialVelocity(item.getInitialVelocity() * factor);
                new BallisticSolver(item).solve();
            }
            double newV = ctx.collection.getAll().get(0).getInitialVelocity();
            report("ScaleVelocityCommand: v0 помножено на " + factor,
                Math.abs(newV - origV * factor) < EPS,
                String.format("очікувано=%.2f отримано=%.2f", origV * factor, newV));
        }

        /** undo() відновлює оригінальні швидкості. */
        private void testScaleCommandUndo() {
            AppContext ctx = makeCtx();
            BallisticData d = new BallisticData(150, 30);
            new BallisticSolver(d).solve();
            ctx.collection.add(d);

            List<BallisticData> snap = new ArrayList<>();
            snap.add(copyData(d));
            double origV = d.getInitialVelocity();

            // Масштабуємо і відновлюємо
            for (BallisticData item : ctx.collection.getMutable())
                item.setInitialVelocity(item.getInitialVelocity() * 3.0);
            ctx.collection.setAll(snap); // undo simulation
            double restoredV = ctx.collection.getAll().get(0).getInitialVelocity();
            report("ScaleVelocityCommand undo: v0 відновлено",
                Math.abs(restoredV - origV) < EPS,
                String.format("orig=%.2f restored=%.2f", origV, restoredV));
        }

        // ----- Група 5: NormalizeRangesCommand -----

        /** Після нормалізації мін=0, макс=1. */
        private void testNormalizeMinIsZeroMaxIsOne() {
            AppContext ctx = makeFilledCtx();
            NormalizeRangesCommand cmd = new NormalizeRangesCommand(ctx);
            cmd.execute();
            double min = ctx.collection.getAll().stream()
                .mapToDouble(BallisticData::getRangeMetric).min().orElse(-1);
            double max = ctx.collection.getAll().stream()
                .mapToDouble(BallisticData::getRangeMetric).max().orElse(-1);
            report("NormalizeRanges: min ≈ 0", Math.abs(min) < EPS, "min=" + min);
            report("NormalizeRanges: max ≈ 1", Math.abs(max - 1.0) < EPS, "max=" + max);
        }

        /** undo() відновлює оригінальні дальності. */
        private void testNormalizeUndo() {
            AppContext ctx = makeFilledCtx();
            double origFirst = ctx.collection.getAll().get(0).getRangeMetric();
            NormalizeRangesCommand cmd = new NormalizeRangesCommand(ctx);
            cmd.execute();
            cmd.undo();
            double restored = ctx.collection.getAll().get(0).getRangeMetric();
            report("NormalizeRanges undo: значення відновлено",
                Math.abs(restored - origFirst) < EPS,
                String.format("orig=%.4f restored=%.4f", origFirst, restored));
        }

        // ----- Група 6: ClearCommand -----

        /** execute() очищає колекцію. */
        private void testClearCommandEmptiesCollection() {
            AppContext ctx = makeFilledCtx();
            ClearCommand cmd = new ClearCommand(ctx);
            cmd.execute();
            report("ClearCommand: колекція порожня", ctx.collection.size() == 0,
                "розмір=" + ctx.collection.size());
        }

        /** undo() відновлює колекцію. */
        private void testClearCommandUndo() {
            AppContext ctx = makeFilledCtx();
            int before = ctx.collection.size();
            ClearCommand cmd = new ClearCommand(ctx);
            cmd.execute();
            cmd.undo();
            report("ClearCommand undo: колекцію відновлено",
                ctx.collection.size() == before,
                "до=" + before + " після=" + ctx.collection.size());
        }

        // ----- Група 7: MacroCommand -----

        /** MacroCommand виконує всі підкоманди. */
        private void testMacroCommandExecutesAll() {
            AppContext ctx = makeCtx();
            List<boolean[]> flags = Arrays.asList(new boolean[]{false}, new boolean[]{false});
            Command c1 = new Command() {
                public void execute() { flags.get(0)[0] = true; }
                public void undo()    {}
                public String getName() { return "C1"; }
            };
            Command c2 = new Command() {
                public void execute() { flags.get(1)[0] = true; }
                public void undo()    {}
                public String getName() { return "C2"; }
            };
            MacroCommand macro = new MacroCommand("TestMacro", Arrays.asList(c1, c2));
            macro.execute();
            report("MacroCommand: всі підкоманди виконані",
                flags.get(0)[0] && flags.get(1)[0], "");
        }

        /** MacroCommand undo() скасовує у зворотному порядку. */
        private void testMacroCommandUndoReverse() {
            List<Integer> order = new ArrayList<>();
            Command c1 = new Command() {
                public void execute() {}
                public void undo()    { order.add(1); }
                public String getName() { return "C1"; }
            };
            Command c2 = new Command() {
                public void execute() {}
                public void undo()    { order.add(2); }
                public String getName() { return "C2"; }
            };
            MacroCommand macro = new MacroCommand("UndoOrder", Arrays.asList(c1, c2));
            macro.execute();
            macro.undo();
            // Undo має бути: c2 спочатку, c1 потім → [2, 1]
            report("MacroCommand undo: зворотний порядок",
                order.size() == 2 && order.get(0) == 2 && order.get(1) == 1,
                "порядок=" + order);
        }

        // ----- Група 8: Menu -----

        /** Динамічне додавання і видалення пунктів. */
        private void testMenuAddRemoveEntry() {
            Menu menu = new Menu("Test");
            menu.addEntry("1", "Тест", new Command() {
                public void execute() {}
                public void undo()    {}
                public String getName() { return "Тест"; }
            });
            boolean hasBefore = menu.hasKey("1");
            menu.removeEntry("1");
            boolean hasAfter = menu.hasKey("1");
            report("Menu: addEntry/removeEntry", hasBefore && !hasAfter, "");
        }

        /** executeCommand() виконує команду за ключем. */
        private void testMenuExecuteCommand() {
            boolean[] ran = {false};
            Menu menu = new Menu("Test");
            menu.addEntry("X", "Команда", new Command() {
                public void execute() { ran[0] = true; }
                public void undo()    {}
                public String getName() { return "X"; }
            });
            CommandHistory.getInstance().clearHistory();
            boolean found = menu.executeCommand("X");
            CommandHistory.getInstance().clearHistory();
            report("Menu.executeCommand: команду виконано", found && ran[0], "");
        }

        // ----- Група 9: SearchByRangeCommand -----

        /** Пошук знаходить записи в діапазоні. */
        private void testSearchFindsInRange() {
            AppContext ctx = makeFilledCtx();
            long count = ctx.collection.getAll().stream()
                .filter(d -> d.getRangeMetric() >= 0 && d.getRangeMetric() <= 1e9)
                .count();
            report("SearchByRangeCommand: знайдено у широкому діапазоні",
                count == ctx.collection.size(), "знайдено=" + count);
        }

        /** Пошук у порожньому діапазоні дає 0. */
        private void testSearchEmptyResult() {
            AppContext ctx = makeFilledCtx();
            long count = ctx.collection.getAll().stream()
                .filter(d -> d.getRangeMetric() < 0)
                .count();
            report("SearchByRangeCommand: порожній результат для від'ємного діапазону",
                count == 0, "знайдено=" + count);
        }

        // ----- Група 10: InterpolateCommand -----

        /** execute() додає вказану кількість точок. */
        private void testInterpolateAddsPoints() {
            AppContext ctx = makeFilledCtx();
            int before = ctx.collection.size();
            InterpolateCommand cmd = new InterpolateCommand(ctx) {
                @Override public void execute() {
                    int n = 3;
                    List<BallisticData> all  = ctx.collection.getAll();
                    BallisticData first = all.get(0);
                    BallisticData last  = all.get(all.size() - 1);
                    for (int i = 1; i <= n; i++) {
                        double t  = (double) i / (n + 1);
                        double v0 = first.getInitialVelocity()
                            + t * (last.getInitialVelocity() - first.getInitialVelocity());
                        double a  = Math.max(0, Math.min(90,
                            first.getAngleDegrees()
                            + t * (last.getAngleDegrees() - first.getAngleDegrees())));
                        BallisticData d = new BallisticData(v0, a);
                        new BallisticSolver(d).solve();
                        ctx.collection.add(d);
                    }
                }
            };
            cmd.execute();
            report("InterpolateCommand: додано 3 точки",
                ctx.collection.size() == before + 3,
                "до=" + before + " після=" + ctx.collection.size());
        }

        /** undo() видаляє інтерпольовані точки. */
        private void testInterpolateUndo() {
            AppContext ctx = makeFilledCtx();
            int before = ctx.collection.size();
            // Додаємо 2 точки і відкочуємося
            for (int i = 0; i < 2; i++) {
                BallisticData d = new BallisticData(50 + i * 10, 40);
                new BallisticSolver(d).solve();
                ctx.collection.add(d);
            }
            for (int i = 0; i < 2; i++) ctx.collection.removeLast();
            report("InterpolateCommand undo: розмір відновлено",
                ctx.collection.size() == before, "до=" + before + " після=" + ctx.collection.size());
        }

        // ----- Утиліти -----

        /** Створює порожній контекст з dummy-сканером. */
        private AppContext makeCtx() {
            return new AppContext(new Scanner(""));
        }

        /** Створює контекст з трьома записами різних v0 та α. */
        private AppContext makeFilledCtx() {
            AppContext ctx = makeCtx();
            double[][] params = {{300, 20}, {100, 45}, {500, 60}, {200, 30}};
            for (double[] p : params) {
                BallisticData d = new BallisticData(p[0], p[1]);
                new BallisticSolver(d).solve();
                ctx.collection.add(d);
            }
            return ctx;
        }

        private void report(String name, boolean ok, String details) {
            System.out.printf("  %s : %s", ok ? "[OK]   PASS" : "[FAIL] FAIL", name);
            if (!details.isEmpty()) System.out.printf("  [%s]", details);
            System.out.println();
            if (ok) passed++; else failed++;
        }
    }

    // =========================================================================
    // Утиліти
    // =========================================================================

    /** Ім'я файлу колекції за замовчуванням. */
    // (COLLECTION_FILE перенесено до AppContext)

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
     * Точка входу: тести → інтерактивне меню на базі шаблону Command.
     *
     * <p>Меню побудоване як розширюваний контейнер команд ({@link Menu}).
     * Кожен пункт — об'єкт, що реалізує {@link Command}. Undo доступний
     * для будь-якої операції через Singleton {@link CommandHistory}.</p>
     *
     * <p>Пункти меню:</p>
     * <ol>
     *   <li>Ввести параметри та обчислити.</li>
     *   <li>Показати останній результат.</li>
     *   <li>Показати всю колекцію.</li>
     *   <li>Показати з лімітом рядків (overloading).</li>
     *   <li>Змінити формат відображення (Factory Method).</li>
     *   <li>Налаштувати параметри таблиці (TableConfig).</li>
     *   <li>Сортувати колекцію.</li>
     *   <li>Масштабувати швидкості.</li>
     *   <li>Нормалізувати дальності.</li>
     *   <li>Інтерполювати між крайніми.</li>
     *   <li>Пошук за діапазоном дальності.</li>
     *   <li>Зберегти колекцію.</li>
     *   <li>Завантажити колекцію.</li>
     *   <li>Очистити колекцію.</li>
     *   <li>Демонстрація поліморфізму.</li>
     *   <li>Макрокоманда (Додати → Сортувати → Відобразити).</li>
     *   <li>Undo останньої операції.</li>
     *   <li>Вихід.</li>
     * </ol>
     *
     * @param args аргументи командного рядка (не використовуються)
     * @throws Exception будь-яка непередбачена помилка
     */
    public static void main(String[] args) throws Exception {

        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        System.setErr(new PrintStream(System.err, true, "UTF-8"));

        // --- Тести (v3: базові + Command) ---
        System.out.println("\n>>> Запуск базових тестів...");
        boolean t1 = new BallisticTest().runAll();
        if (!t1) System.err.println("[!] Деякі базові тести провалено.");

        System.out.println("\n>>> Запуск тестів Command/Singleton/Macro...");
        boolean t2 = new BallisticCommandTest().runAll();
        if (!t2) System.err.println("[!] Деякі Command-тести провалено.");

        // --- Ініціалізація контексту ---
        Scanner scanner = new Scanner(System.in);
        AppContext ctx  = new AppContext(scanner);

        // --- Побудова меню (Command pattern) ---
        Menu menu = new Menu(
            "Балістичний калькулятор v4.0 — Command + Singleton + Macro");

        // Основні операції
        menu.addEntry("1",  "Ввести параметри та обчислити",
            new AddDataCommand(ctx));
        menu.addEntry("2",  "Показати останній результат",
            new ShowLastCommand(ctx));
        menu.addEntry("3",  "Показати всю колекцію",
            new PrintCollectionCommand(ctx));

        // Overloading демонстрація
        menu.addEntry("4",  "Показати з лімітом рядків (overloading)",
            new Command() {
                public String getName() { return "Показати колекцію з лімітом (overloading)"; }
                public void execute() {
                    if (ctx.collection.size() == 0) { System.out.println("  [!] Порожньо."); return; }
                    int lim = readInt(ctx.scanner, "  > Ліміт рядків: ");
                    BallisticData ref = ctx.lastData != null
                        ? ctx.lastData : ctx.collection.getAll().get(0);
                    Displayable d = ctx.factory.createDisplay(ref);
                    System.out.println("[Overloading] displayTable(List, int) — ліміт=" + lim);
                    if (d instanceof ConfigurableDisplay)
                        ((ConfigurableDisplay) d).displayTable(ctx.collection.getAll(), lim);
                    else
                        d.displayTable(ctx.collection.getAll());
                }
                public void undo() {}
            });

        // Налаштування відображення
        menu.addEntry("5",  "Змінити формат відображення (Factory Method)",
            new ChangeFactoryCommand(ctx));
        menu.addEntry("6",  "Налаштувати параметри таблиці (TableConfig)",
            new ConfigureTableCommand(ctx));

        // Алгоритми обробки колекції
        menu.addEntry("7",  "Сортувати за дальністю",
            new SortCommand(ctx, 1));
        menu.addEntry("8",  "Сортувати за швидкістю",
            new SortCommand(ctx, 2));
        menu.addEntry("9",  "Сортувати за кутом",
            new SortCommand(ctx, 3));
        menu.addEntry("10", "Масштабувати швидкості (×коефіцієнт)",
            new ScaleVelocityCommand(ctx));
        menu.addEntry("11", "Нормалізувати дальності до [0, 1]",
            new NormalizeRangesCommand(ctx));
        menu.addEntry("12", "Інтерполювати між крайніми елементами",
            new InterpolateCommand(ctx));
        menu.addEntry("13", "Пошук за діапазоном дальності (м)",
            new SearchByRangeCommand(ctx));

        // Файлові операції
        menu.addEntry("14", "Зберегти колекцію у файл",
            new SaveCommand(ctx));
        menu.addEntry("15", "Завантажити колекцію з файлу",
            new LoadCommand(ctx));
        menu.addEntry("16", "Очистити колекцію",
            new ClearCommand(ctx));

        // Демонстрації
        menu.addEntry("17", "Демонстрація поліморфізму (dynamic dispatch)",
            new PolymorphismDemoCommand(ctx));

        // Макрокоманда: Додати → Сортувати → Відобразити
        menu.addEntry("18", "Макрокоманда: Додати → Сортувати → Відобразити",
            new MacroCommand("Додати+Сортувати+Показати", Arrays.asList(
                new AddDataCommand(ctx),
                new SortCommand(ctx, 1),
                new PrintCollectionCommand(ctx)
            )));

        // Undo
        menu.addEntry("U",  "Скасувати останню операцію (Undo)",
            new Command() {
                public String getName() { return "Undo"; }
                public void execute()   { CommandHistory.getInstance().undo(); }
                public void undo()      { /* undo undo — не підтримується */ }
            });

        // Вихід
        menu.addEntry("0",  "Вихід", new Command() {
            public String getName() { return "Вихід"; }
            public void execute()   { /* handled below */ }
            public void undo()      {}
        });

        // --- Головний цикл ---
        System.out.println("\n+============================================================+");
        System.out.println("|  Балістичний калькулятор v4.0                              |");
        System.out.println("|  Command + Singleton + Macro + Factory Method              |");
        System.out.println("+============================================================+");

        while (true) {
            menu.display(ctx);
            String choice = scanner.nextLine().trim().toUpperCase();

            if (choice.equals("0")) {
                System.out.println("\n[OK] Завершення роботи. Ракета полетіла.");
                break;
            }

            if (!menu.executeCommand(choice)) {
                System.out.println("  [!] Невідомий пункт меню: «" + choice + "»");
            }
        }

        scanner.close();
    }
}