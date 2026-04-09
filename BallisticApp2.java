import java.io.*;
import java.util.*;

/**
 * Головний клас для демонстрації серіалізації та десеріалізації об'єктів,
 * обчислення дальності польоту по балістичній траєкторії, зберігання
 * результатів у колекції, а також шаблону проектування Factory Method.
 *
 * <p>Формула: {@code s = v0² · sin(2α) / g}</p>
 *
 * <p>Містить вкладені класи та інтерфейси:</p>
 * <ul>
 *   <li>{@link BallisticData} — серіалізований клас даних з transient-полями.</li>
 *   <li>{@link BallisticSolver} — клас-розв'язувач (агрегує BallisticData).</li>
 *   <li>{@link ResultCollection} — колекція результатів з серіалізацією.</li>
 *   <li>{@link Displayable} — інтерфейс відображення результатів.</li>
 *   <li>{@link DisplayFactory} — інтерфейс фабрики відображень.</li>
 *   <li>{@link TextDisplay} — текстове відображення.</li>
 *   <li>{@link DetailedTextDisplay} — детальне текстове відображення.</li>
 *   <li>{@link CompactDisplay} — компактне відображення.</li>
 *   <li>{@link TextDisplayFactory} — фабрика текстових відображень.</li>
 *   <li>{@link DetailedDisplayFactory} — фабрика детальних відображень.</li>
 *   <li>{@link CompactDisplayFactory} — фабрика компактних відображень.</li>
 *   <li>{@link BallisticTest} — автоматичне тестування.</li>
 * </ul>
 *
 * @author Eduard
 * @version 2.0
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
                throw new IllegalArgumentException("Початкова швидкість не може бути від'ємною: " + initialVelocity);
            if (angleDegrees < 0 || angleDegrees > 90)
                throw new IllegalArgumentException("Кут має бути в діапазоні [0; 90]: " + angleDegrees);
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
                timestamp, statusMessage
            );
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

        /**
         * Агрегований об'єкт даних.
         */
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
         * <p>{@code s = v0² · sin(2α) / g}, де {@code g = 9.80665 м/с²}.</p>
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
         * <p>Швидкість конвертується у фут/с, {@code g = 32.174 фут/с²}.</p>
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
                data.getRangeImperial(), data.getRangeImperial() / 5280.0
            );
        }
    }

    // =========================================================================
    // ResultCollection
    // =========================================================================

    /**
     * Колекція результатів балістичних обчислень з підтримкою
     * серіалізації/десеріалізації всього списку.
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
         * Додає результат обчислення до колекції.
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
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
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
        public void loadFromFile(String filename) throws IOException, ClassNotFoundException {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
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
    // Displayable — інтерфейс відображення (фабрикований продукт)
    // =========================================================================

    /**
     * Інтерфейс для об'єктів, що відображають результати балістичних обчислень.
     *
     * <p>Реалізує роль <b>Product</b> у шаблоні Factory Method.
     * Кожна реалізація надає три методи виведення: короткий, повний та табличний.</p>
     *
     * @see DisplayFactory
     * @see TextDisplay
     * @see DetailedTextDisplay
     * @see CompactDisplay
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
         * Виводить всі результати колекції у табличному вигляді.
         *
         * @param results список {@link BallisticData} для відображення
         */
        void displayTable(List<BallisticData> results);
    }

    // =========================================================================
    // DisplayFactory — інтерфейс фабрики (Creator)
    // =========================================================================

    /**
     * Інтерфейс фабрики для створення об'єктів {@link Displayable}.
     *
     * <p>Реалізує роль <b>Creator</b> у шаблоні Factory Method.
     * Підкласи перевизначають {@link #createDisplay(BallisticData)}, повертаючи
     * конкретну реалізацію {@link Displayable} без зміни клієнтського коду.</p>
     *
     * @see TextDisplayFactory
     * @see DetailedDisplayFactory
     * @see CompactDisplayFactory
     */
    interface DisplayFactory {

        /**
         * Фабричний метод: створює об'єкт відображення для заданих даних.
         *
         * @param data об'єкт {@link BallisticData} для відображення
         * @return конкретний {@link Displayable}
         */
        Displayable createDisplay(BallisticData data);

        /**
         * Шаблонний метод: виконує повний цикл відображення.
         * Викликає {@link #createDisplay(BallisticData)} та виводить повний звіт.
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
    // TextDisplay — конкретний продукт
    // =========================================================================

    /**
     * Текстове відображення результатів балістичних обчислень.
     *
     * <p>Конкретний <b>Product</b> у шаблоні Factory Method.
     * Виводить результати у зручному читабельному форматі.</p>
     *
     * @see TextDisplayFactory
     */
    static class TextDisplay implements Displayable {

        /** Дані для відображення. */
        private final BallisticData data;

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
            System.out.printf("  Початкова швидкість : %.2f м/с%n",    data.getInitialVelocity());
            System.out.printf("  Кут пострілу        : %.2f°%n",        data.getAngleDegrees());
            System.out.printf("  Дальність (м)       : %.4f м%n",       data.getRangeMetric());
            System.out.printf("  Дальність (км)      : %.6f км%n",      data.getRangeMetric() / 1000.0);
            System.out.printf("  Дальність (фут)     : %.4f фут%n",     data.getRangeImperial());
            System.out.printf("  Дальність (миль)    : %.6f миль%n",    data.getRangeImperial() / 5280.0);
            System.out.println("------------------------------------------");
        }

        /**
         * Виводить усі результати колекції нумерованим списком.
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
                    System.out.printf("  %2d. v0=%.1f м/с, α=%.1f°  →  %.2f м / %.2f фут%n",
                        i + 1, r.getInitialVelocity(), r.getAngleDegrees(),
                        r.getRangeMetric(), r.getRangeImperial());
                }
            }
            System.out.println("=============================================");
        }
    }

    // =========================================================================
    // DetailedTextDisplay — конкретний продукт
    // =========================================================================

    /**
     * Детальне текстове відображення з розширеним форматом та коментарями.
     *
     * <p>Конкретний <b>Product</b> у шаблоні Factory Method.
     * Виводить формулу, проміжні обчислення та пояснення.</p>
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

        /**
         * Виводить короткий рядок із позначкою «Детально».
         */
        @Override
        public void displayShort() {
            System.out.printf("[Детально] v0=%.1f м/с | α=%.1f° | дальність=%.4f м | %.4f фут%n",
                data.getInitialVelocity(), data.getAngleDegrees(),
                data.getRangeMetric(), data.getRangeImperial());
        }

        /**
         * Виводить повний детальний звіт із формулою та проміжними значеннями.
         */
        @Override
        public void displayFull() {
            double v0       = data.getInitialVelocity();
            double alpha    = data.getAngleDegrees();
            double alphaRad = Math.toRadians(alpha);
            double sin2a    = Math.sin(2.0 * alphaRad);

            System.out.println("====== Детальний звіт (DetailedTextDisplay) ======");
            System.out.println("  Формула: s = v0² · sin(2α) / g");
            System.out.println("  ------ Вхідні параметри ------");
            System.out.printf("    v0        = %.4f м/с%n",     v0);
            System.out.printf("    α         = %.4f° (%.6f рад)%n", alpha, alphaRad);
            System.out.println("  ------ Проміжні обчислення ------");
            System.out.printf("    v0²       = %.4f (м/с)²%n",  v0 * v0);
            System.out.printf("    sin(2α)   = %.6f%n",         sin2a);
            System.out.printf("    g_metric  = %.5f м/с²%n",    BallisticData.G_METRIC);
            System.out.printf("    g_imperial= %.3f фут/с²%n",  BallisticData.G_IMPERIAL);
            System.out.println("  ------ Результати ------");
            System.out.printf("    Метрична система  : %.6f м%n",     data.getRangeMetric());
            System.out.printf("                        %.6f км%n",    data.getRangeMetric() / 1000.0);
            System.out.printf("    Англійська система: %.6f фут%n",   data.getRangeImperial());
            System.out.printf("                        %.6f миль%n",  data.getRangeImperial() / 5280.0);
            System.out.println("==================================================");
        }

        /**
         * Виводить таблицю результатів з усіма значеннями у деталях.
         *
         * @param results список результатів
         */
        @Override
        public void displayTable(List<BallisticData> results) {
            System.out.println("====== Детальна таблиця результатів ======");
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
     * <p>Конкретний <b>Product</b> у шаблоні Factory Method.
     * Виводить лише найважливіші значення у стислому форматі.</p>
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

        /**
         * Виводить ультра-короткий рядок.
         */
        @Override
        public void displayShort() {
            System.out.printf("[Компакт] %.0f м/с @ %.0f° -> %.0f м%n",
                data.getInitialVelocity(), data.getAngleDegrees(), data.getRangeMetric());
        }

        /**
         * Виводить компактний однорядковий звіт.
         */
        @Override
        public void displayFull() {
            System.out.printf("[ CompactDisplay ] v0=%.2f | α=%.2f° | s=%.2f м | s=%.2f фут%n",
                data.getInitialVelocity(), data.getAngleDegrees(),
                data.getRangeMetric(), data.getRangeImperial());
        }

        /**
         * Виводить усі результати колекції в одну колонку.
         *
         * @param results список результатів
         */
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
    // Конкретні фабрики
    // =========================================================================

    /**
     * Фабрика для створення об'єктів {@link TextDisplay}.
     *
     * <p>Конкретний <b>Creator</b> у шаблоні Factory Method.</p>
     *
     * @see TextDisplay
     */
    static class TextDisplayFactory implements DisplayFactory {

        /**
         * Створює об'єкт {@link TextDisplay}.
         *
         * @param data дані для відображення
         * @return новий {@link TextDisplay}
         */
        @Override
        public Displayable createDisplay(BallisticData data) {
            return new TextDisplay(data);
        }
    }

    /**
     * Фабрика для створення об'єктів {@link DetailedTextDisplay}.
     *
     * <p>Конкретний <b>Creator</b> у шаблоні Factory Method.</p>
     *
     * @see DetailedTextDisplay
     */
    static class DetailedDisplayFactory implements DisplayFactory {

        /**
         * Створює об'єкт {@link DetailedTextDisplay}.
         *
         * @param data дані для відображення
         * @return новий {@link DetailedTextDisplay}
         */
        @Override
        public Displayable createDisplay(BallisticData data) {
            return new DetailedTextDisplay(data);
        }
    }

    /**
     * Фабрика для створення об'єктів {@link CompactDisplay}.
     *
     * <p>Конкретний <b>Creator</b> у шаблоні Factory Method.</p>
     *
     * @see CompactDisplay
     */
    static class CompactDisplayFactory implements DisplayFactory {

        /**
         * Створює об'єкт {@link CompactDisplay}.
         *
         * @param data дані для відображення
         * @return новий {@link CompactDisplay}
         */
        @Override
        public Displayable createDisplay(BallisticData data) {
            return new CompactDisplay(data);
        }
    }

    // =========================================================================
    // BallisticTest
    // =========================================================================

    /**
     * Клас для тестування коректності обчислень, серіалізації,
     * колекції та шаблону Factory Method.
     *
     * <p>Виконує набір автоматичних тестів: граничні кути, відомі значення,
     * узгодженість систем мір, збереження полів, втрата transient-полів,
     * валідація вхідних даних, колекція, фабрики.</p>
     */
    static class BallisticTest {

        /** Допустима похибка для порівняння дійсних чисел. */
        private static final double EPSILON = 1e-6;

        /** Ім'я тимчасового файлу для тестів серіалізації. */
        private static final String TEST_FILE = "test_ballistic.ser";

        /** Ім'я тимчасового файлу для тестів колекції. */
        private static final String TEST_COLL_FILE = "test_collection.ser";

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
            System.out.println("|     Тестування балістичних ракет та обчислень        |");
            System.out.println("+======================================================+\n");

            testAngle45();
            testAngle0();
            testAngle90();
            testKnownValues();
            testMetricImperialConsistency();
            testSerializationRegularFields();
            testTransientFieldsLost();
            testInvalidVelocity();
            testInvalidAngle();
            testNullData();
            testCollectionAddAndSize();
            testCollectionSaveLoad();
            testCollectionClear();
            testTextDisplayFactory();
            testDetailedDisplayFactory();
            testCompactDisplayFactory();

            System.out.println("\n==============================================");
            System.out.printf("  Фіксація по цілям: %d пройдено, %d провалено з %d%n",
                passed, failed, passed + failed);
            System.out.println("==============================================");

            new File(TEST_FILE).delete();
            new File(TEST_COLL_FILE).delete();
            return failed == 0;
        }

        /**
         * Тест: кут 45° дає максимальну дальність.
         */
        private void testAngle45() {
            double v0 = 100.0;
            BallisticSolver solver = new BallisticSolver(v0, 45.0);
            double expected = (v0 * v0) / BallisticData.G_METRIC;
            assertAlmostEqual("Кут 45° — максимальна дальність", expected, solver.computeRangeMetric());
        }

        /** Тест: кут 0° — дальність = 0. */
        private void testAngle0() {
            BallisticSolver solver = new BallisticSolver(100.0, 0.0);
            assertAlmostEqual("Кут 0° — дальність = 0", 0.0, solver.computeRangeMetric());
        }

        /** Тест: кут 90° — дальність ≈ 0. */
        private void testAngle90() {
            BallisticSolver solver = new BallisticSolver(100.0, 90.0);
            assertAlmostEqual("Кут 90° — дальність ≈ 0", 0.0, solver.computeRangeMetric());
        }

        /**
         * Тест: перевірка для v0 = 200 м/с, α = 30°.
         */
        private void testKnownValues() {
            double v0 = 200.0, angle = 30.0;
            double expected = (v0 * v0 * Math.sin(Math.toRadians(60.0))) / BallisticData.G_METRIC;
            BallisticSolver solver = new BallisticSolver(v0, angle);
            solver.solve();
            assertAlmostEqual("Відомі значення (v0=200, α=30°)", expected, solver.getData().getRangeMetric());
        }

        /**
         * Тест: узгодженість метричної та англійської систем (похибка &lt; 0.5%).
         */
        private void testMetricImperialConsistency() {
            BallisticSolver solver = new BallisticSolver(150.0, 60.0);
            solver.solve();
            double metricInFeet = solver.getData().getRangeMetric() * BallisticData.METERS_TO_FEET;
            double imperial     = solver.getData().getRangeImperial();
            double relErr       = Math.abs(metricInFeet - imperial) / imperial;
            report("Узгодженість метрична/англійська (< 0.5%)", relErr < 0.005,
                String.format("відн.похибка=%.6f", relErr));
        }

        /** Тест: звичайні поля зберігаються після серіалізації. */
        private void testSerializationRegularFields() {
            try {
                BallisticData original = new BallisticData(250.0, 35.0);
                new BallisticSolver(original).solve();
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TEST_FILE))) {
                    oos.writeObject(original);
                }
                BallisticData restored;
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TEST_FILE))) {
                    restored = (BallisticData) ois.readObject();
                }
                boolean ok = original.getInitialVelocity() == restored.getInitialVelocity()
                          && original.getAngleDegrees()    == restored.getAngleDegrees()
                          && original.getRangeMetric()     == restored.getRangeMetric()
                          && original.getRangeImperial()   == restored.getRangeImperial();
                report("Серіалізація — збереження звичайних полів", ok, "");
            } catch (Exception e) {
                report("Серіалізація — збереження звичайних полів", false, e.getMessage());
            }
        }

        /**
         * Тест: transient-поля = значення за замовчуванням після десеріалізації.
         */
        private void testTransientFieldsLost() {
            try {
                BallisticData original = new BallisticData(100.0, 45.0);
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(TEST_FILE))) {
                    oos.writeObject(original);
                }
                BallisticData restored;
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(TEST_FILE))) {
                    restored = (BallisticData) ois.readObject();
                }
                report("Transient: timestamp скинуто в 0",   restored.getTimestamp() == 0L,
                    "отримано: " + restored.getTimestamp());
                report("Transient: statusMessage скинуто в null", restored.getStatusMessage() == null,
                    "отримано: " + restored.getStatusMessage());
            } catch (Exception e) {
                report("Transient-поля", false, e.getMessage());
            }
        }

        /** Тест: від'ємна швидкість → {@link IllegalArgumentException}. */
        private void testInvalidVelocity() {
            try {
                new BallisticData(-10.0, 45.0);
                report("Від'ємна швидкість → виняток", false, "Виняток не згенеровано!");
            } catch (IllegalArgumentException e) {
                report("Від'ємна швидкість → виняток", true, e.getMessage());
            }
        }

        /** Тест: кут поза [0; 90] → {@link IllegalArgumentException}. */
        private void testInvalidAngle() {
            boolean c1 = false, c2 = false;
            try { new BallisticData(100.0, -5.0);  } catch (IllegalArgumentException e) { c1 = true; }
            try { new BallisticData(100.0, 100.0); } catch (IllegalArgumentException e) { c2 = true; }
            report("Некоректний кут → виняток", c1 && c2, "");
        }

        /** Тест: {@code null} → {@link IllegalArgumentException} у конструкторі Solver. */
        private void testNullData() {
            try {
                new BallisticSolver(null);
                report("Null data → виняток", false, "Виняток не згенеровано!");
            } catch (IllegalArgumentException e) {
                report("Null data → виняток", true, e.getMessage());
            }
        }

        /** Тест: додавання елементів до колекції та перевірка розміру. */
        private void testCollectionAddAndSize() {
            ResultCollection col = new ResultCollection();
            col.add(new BallisticData(100.0, 30.0));
            col.add(new BallisticData(200.0, 45.0));
            report("ResultCollection: додавання та розмір", col.size() == 2,
                "розмір=" + col.size());
        }

        /** Тест: збереження та відновлення колекції. */
        private void testCollectionSaveLoad() {
            try {
                ResultCollection original = new ResultCollection();
                BallisticData d1 = new BallisticData(100.0, 30.0);
                BallisticData d2 = new BallisticData(200.0, 45.0);
                new BallisticSolver(d1).solve();
                new BallisticSolver(d2).solve();
                original.add(d1);
                original.add(d2);
                original.saveToFile(TEST_COLL_FILE);

                ResultCollection restored = new ResultCollection();
                restored.loadFromFile(TEST_COLL_FILE);

                boolean ok = restored.size() == 2
                    && restored.getAll().get(0).getInitialVelocity() == 100.0
                    && restored.getAll().get(1).getAngleDegrees()    == 45.0;
                report("ResultCollection: збереження/відновлення", ok,
                    "розмір після відновлення=" + restored.size());
            } catch (Exception e) {
                report("ResultCollection: збереження/відновлення", false, e.getMessage());
            }
        }

        /** Тест: очищення колекції. */
        private void testCollectionClear() {
            ResultCollection col = new ResultCollection();
            col.add(new BallisticData(100.0, 45.0));
            col.clear();
            report("ResultCollection: очищення", col.size() == 0, "розмір=" + col.size());
        }

        /** Тест: TextDisplayFactory створює TextDisplay. */
        private void testTextDisplayFactory() {
            DisplayFactory factory = new TextDisplayFactory();
            BallisticData d = new BallisticData(100.0, 45.0);
            new BallisticSolver(d).solve();
            Displayable display = factory.createDisplay(d);
            report("TextDisplayFactory — тип TextDisplay",
                display instanceof TextDisplay, display.getClass().getSimpleName());
        }

        /** Тест: DetailedDisplayFactory створює DetailedTextDisplay. */
        private void testDetailedDisplayFactory() {
            DisplayFactory factory = new DetailedDisplayFactory();
            BallisticData d = new BallisticData(150.0, 30.0);
            new BallisticSolver(d).solve();
            Displayable display = factory.createDisplay(d);
            report("DetailedDisplayFactory — тип DetailedTextDisplay",
                display instanceof DetailedTextDisplay, display.getClass().getSimpleName());
        }

        /** Тест: CompactDisplayFactory створює CompactDisplay. */
        private void testCompactDisplayFactory() {
            DisplayFactory factory = new CompactDisplayFactory();
            BallisticData d = new BallisticData(200.0, 60.0);
            new BallisticSolver(d).solve();
            Displayable display = factory.createDisplay(d);
            report("CompactDisplayFactory — тип CompactDisplay",
                display instanceof CompactDisplay, display.getClass().getSimpleName());
        }

        /** Порівнює два числа з похибкою {@link #EPSILON}. */
        private void assertAlmostEqual(String name, double expected, double actual) {
            report(name, Math.abs(expected - actual) < EPSILON,
                String.format("очікувано=%.6f, отримано=%.6f", expected, actual));
        }

        /** Виводить результат тесту. */
        private void report(String name, boolean success, String details) {
            System.out.printf("  %s : %s", success ? "[OK]   PASS" : "[FAIL] FAIL", name);
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
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Некоректне число, спробуйте ще раз.");
            }
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
            try {
                return Integer.parseInt(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("  [!] Некоректне ціле число, спробуйте ще раз.");
            }
        }
    }

    /**
     * Вибирає фабрику відображення за номером.
     *
     * @param choice номер фабрики (1 — текст, 2 — детальний, 3 — компактний)
     * @return відповідна {@link DisplayFactory}
     */
    private static DisplayFactory chooseFactory(int choice) {
        switch (choice) {
            case 2:  return new DetailedDisplayFactory();
            case 3:  return new CompactDisplayFactory();
            default: return new TextDisplayFactory();
        }
    }

    // =========================================================================
    // main
    // =========================================================================

    /**
     * Точка входу: запускає тести, потім інтерактивне меню.
     *
     * <p>Можливості меню:</p>
     * <ol>
     *   <li>Ввести нові параметри та обчислити.</li>
     *   <li>Вибрати формат відображення (Factory Method).</li>
     *   <li>Переглянути всі результати колекції.</li>
     *   <li>Зберегти колекцію у файл.</li>
     *   <li>Завантажити колекцію з файлу.</li>
     *   <li>Очистити колекцію.</li>
     *   <li>Вийти.</li>
     * </ol>
     *
     * @param args аргументи командного рядка (не використовуються)
     * @throws Exception будь-яка непередбачена помилка
     */
    public static void main(String[] args) throws Exception {

        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        System.setErr(new PrintStream(System.err, true, "UTF-8"));

        // --- Автоматичне тестування ---
        BallisticTest tester = new BallisticTest();
        boolean allPassed = tester.runAll();
        if (!allPassed) {
            System.err.println("\n[!] Деякі тести провалені.");
        }

        // --- Інтерактивне меню ---
        Scanner scanner = new Scanner(System.in);
        ResultCollection collection = new ResultCollection();
        DisplayFactory factory = new TextDisplayFactory();
        BallisticData lastData = null;

        System.out.println("\n+============================================================+");
        System.out.println("|  Балістичний калькулятор — Factory Method + колекція       |");
        System.out.println("+============================================================+");

        boolean running = true;
        while (running) {
            System.out.println("\n+--- Меню -------------------------------------------+");
            System.out.println("  1. Ввести параметри та обчислити дальність");
            System.out.println("  2. Вибрати формат відображення (Factory Method)");
            System.out.println("  3. Показати останній результат");
            System.out.println("  4. Показати всі результати колекції");
            System.out.println("  5. Зберегти колекцію у файл");
            System.out.println("  6. Завантажити колекцію з файлу");
            System.out.println("  7. Очистити колекцію");
            System.out.println("  0. Вихід");
            System.out.println("+----------------------------------------------------+");

            int choice = readInt(scanner, "> Виберіть пункт: ");

            switch (choice) {

                case 1: {
                    double velocity, angle;
                    try {
                        velocity = readDouble(scanner, "> Початкова швидкість v0 (м/с): ");
                        angle    = readDouble(scanner, "> Кут α до горизонту (°, 0–90): ");
                    } catch (Exception e) {
                        System.out.println("  [!] " + e.getMessage());
                        break;
                    }
                    try {
                        BallisticData data   = new BallisticData(velocity, angle);
                        BallisticSolver solver = new BallisticSolver(data);
                        solver.solve();
                        collection.add(data);
                        lastData = data;
                        System.out.println("\n" + solver.getReport());
                        System.out.printf("[OK] Результат додано до колекції (всього: %d)%n", collection.size());
                    } catch (IllegalArgumentException e) {
                        System.out.println("  [!] Помилка вхідних даних: " + e.getMessage());
                    }
                    break;
                }

                case 2: {
                    System.out.println("\n  Оберіть формат відображення:");
                    System.out.println("    1. TextDisplay        — стандартний текст");
                    System.out.println("    2. DetailedTextDisplay — детально з формулою");
                    System.out.println("    3. CompactDisplay     — компактний");
                    int fmt = readInt(scanner, "  > Формат: ");
                    factory = chooseFactory(fmt);
                    System.out.printf("[OK] Обрано фабрику: %s%n", factory.getClass().getSimpleName());
                    break;
                }

                case 3: {
                    if (lastData == null) {
                        System.out.println("  [!] Ще немає обчислених результатів.");
                    } else {
                        Displayable d = factory.createDisplay(lastData);
                        System.out.println();
                        d.displayFull();
                    }
                    break;
                }

                case 4: {
                    if (collection.size() == 0) {
                        System.out.println("  [!] Колекція порожня.");
                    } else {
                        BallisticData ref = lastData != null ? lastData : collection.getAll().get(0);
                        Displayable d = factory.createDisplay(ref);
                        System.out.println();
                        d.displayTable(collection.getAll());
                    }
                    break;
                }

                case 5: {
                    try {
                        collection.saveToFile(COLLECTION_FILE);
                        System.out.printf("[OK] Колекцію збережено у \"%s\" (%d записів)%n",
                            COLLECTION_FILE, collection.size());
                    } catch (IOException e) {
                        System.out.println("  [!] Помилка збереження: " + e.getMessage());
                    }
                    break;
                }

                case 6: {
                    try {
                        collection.loadFromFile(COLLECTION_FILE);
                        System.out.printf("[OK] Колекцію завантажено з \"%s\" (%d записів)%n",
                            COLLECTION_FILE, collection.size());
                        if (collection.size() > 0) {
                            lastData = collection.getAll().get(collection.size() - 1);
                        }
                    } catch (FileNotFoundException e) {
                        System.out.printf("  [!] Файл \"%s\" не знайдено. Спочатку збережіть колекцію.%n",
                            COLLECTION_FILE);
                    } catch (IOException | ClassNotFoundException e) {
                        System.out.println("  [!] Помилка завантаження: " + e.getMessage());
                    }
                    break;
                }

                case 7: {
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