import java.io.*;
import java.util.Scanner;

/**
 * Головний клас для демонстрації серіалізації та десеріалізації об'єктів,
 * обчислення дальності польоту по балістичній траєкторії,
 * а також тестування коректності результатів.
 *
 * <p>Формула: {@code s = v0² · sin(2α) / g}</p>
 *
 * <p>Містить вкладені класи:</p>
 * <ul>
 *   <li>{@link BallisticData} — серіалізований клас даних з transient-полями.</li>
 *   <li>{@link BallisticSolver} — клас-розв'язувач (агрегує BallisticData).</li>
 *   <li>{@link BallisticTest} — автоматичне тестування обчислень та серіалізації.</li>
 * </ul>
 *
 * @author Eduard
 * @version 1.0
 */
public class BallisticApp {

    /**
     * Клас для зберігання параметрів та результатів обчислення
     * дальності польоту по балістичній траєкторії.
     *
     * <p>Реалізує інтерфейс {@link Serializable} для підтримки
     * серіалізації/десеріалізації. Поля {@code timestamp} та
     * {@code statusMessage} оголошені як {@code transient} і
     * <b>не зберігаються</b> при серіалізації.</p>
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
         * Мітка часу створення об'єкта (мілісекунди).
         *
         * <p>Оголошено як {@code transient} — це поле <b>не серіалізується</b>.
         * Після десеріалізації значення буде {@code 0L}, що демонструє
         * особливість використання transient-полів.</p>
         */
        private transient long timestamp;

        /**
         * Допоміжне повідомлення про останню операцію.
         *
         * <p>Оголошено як {@code transient} — не зберігається при серіалізації.
         * Після десеріалізації значення буде {@code null}.</p>
         */
        private transient String statusMessage;

        /**
         * Створює об'єкт з початковими параметрами.
         *
         * @param initialVelocity початкова швидкість (м/с), має бути &ge; 0
         * @param angleDegrees    кут до поверхні Землі (градуси), від 0 до 90
         * @throws IllegalArgumentException якщо параметри поза допустимим діапазоном
         */
        public BallisticData(double initialVelocity, double angleDegrees) {
            if (initialVelocity < 0) {
                throw new IllegalArgumentException(
                        "Початкова швидкість не може бути від'ємною: " + initialVelocity);
            }
            if (angleDegrees < 0 || angleDegrees > 90) {
                throw new IllegalArgumentException(
                        "Кут має бути в діапазоні [0; 90]: " + angleDegrees);
            }
            this.initialVelocity = initialVelocity;
            this.angleDegrees = angleDegrees;
            this.rangeMetric = 0.0;
            this.rangeImperial = 0.0;
            this.timestamp = System.currentTimeMillis();
            this.statusMessage = "Об'єкт створено, обчислення ще не виконано.";
        }

        /** @return початкова швидкість (м/с) */
        public double getInitialVelocity() { return initialVelocity; }

        /** @param v швидкість (м/с), має бути &ge; 0 */
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

    /**
     * Клас для обчислення дальності польоту по балістичній траєкторії.
     *
     * <p>Використовує <b>агрегування</b>: містить посилання на об'єкт
     * {@link BallisticData}, який зберігає вхідні параметри та результати.
     * Клас-розв'язувач відповідає лише за логіку обчислень.</p>
     *
     * <p>Формула дальності польоту:
     * {@code s = v0² · sin(2α) / g}</p>
     *
     * @see BallisticData
     */
    static class BallisticSolver {

        /**
         * Агрегований об'єкт даних.
         * Демонстрація патерну агрегування: {@code BallisticSolver}
         * не є підкласом {@code BallisticData}, а містить його як поле.
         */
        private BallisticData data;

        /**
         * Створює розв'язувач із заданим об'єктом даних.
         *
         * @param data об'єкт параметрів; не може бути {@code null}
         * @throws IllegalArgumentException якщо {@code data} є {@code null}
         */
        public BallisticSolver(BallisticData data) {
            if (data == null) {
                throw new IllegalArgumentException("Об'єкт даних не може бути null.");
            }
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
            double v0 = data.getInitialVelocity();
            double angleRad = Math.toRadians(data.getAngleDegrees());
            return (v0 * v0 * Math.sin(2.0 * angleRad)) / BallisticData.G_METRIC;
        }

        /**
         * Обчислює дальність в англійській системі (фути).
         * <p>Швидкість конвертується у фут/с, використовується
         * {@code g = 32.174 фут/с²}.</p>
         *
         * @return дальність (фут)
         */
        public double computeRangeImperial() {
            double v0Feet = data.getInitialVelocity() * BallisticData.METERS_TO_FEET;
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

    /**
     * Клас для тестування коректності обчислень та серіалізації/десеріалізації.
     *
     * <p>Виконує набір автоматичних тестів: граничні кути, відомі значення,
     * узгодженість систем мір, збереження полів, втрата transient-полів,
     * валідація вхідних даних.</p>
     */
    static class BallisticTest {

        /** Допустима похибка для порівняння дійсних чисел. */
        private static final double EPSILON = 1e-6;

        /** Ім'я тимчасового файлу для тестів серіалізації. */
        private static final String TEST_FILE = "test_ballistic.ser";

        /** Лічильник пройдених тестів. */
        private int passed = 0;

        /** Лічильник провалених тестів. */
        private int failed = 0;

        /**
         * Запускає всі тести та повертає {@code true}, якщо всі пройдені.
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

            System.out.println("\n==============================================");
            System.out.printf("  Фіксація по цілям: %d пройдено, %d провалено з %d%n",
                    passed, failed, passed + failed);
            System.out.println("==============================================");

            new File(TEST_FILE).delete();
            return failed == 0;
        }

        /**
         * Тест: кут 45° дає максимальну дальність.
         * При α = 45° → {@code sin(90°) = 1} → {@code s = v0² / g}.
         */
        private void testAngle45() {
            double v0 = 100.0;
            BallisticSolver solver = new BallisticSolver(v0, 45.0);
            double expected = (v0 * v0) / BallisticData.G_METRIC;
            assertAlmostEqual("Кут 45° — максимальна дальність", expected, solver.computeRangeMetric());
        }

        /** Тест: кут 0° — дальність = 0 ({@code sin(0°) = 0}). */
        private void testAngle0() {
            BallisticSolver solver = new BallisticSolver(100.0, 0.0);
            assertAlmostEqual("Кут 0° — дальність = 0", 0.0, solver.computeRangeMetric());
        }

        /** Тест: кут 90° — дальність ≈ 0 (вертикальний постріл, {@code sin(180°) = 0}). */
        private void testAngle90() {
            BallisticSolver solver = new BallisticSolver(100.0, 90.0);
            assertAlmostEqual("Кут 90° — дальність ≈ 0", 0.0, solver.computeRangeMetric());
        }

        /**
         * Тест: перевірка для відомих значень v0 = 200 м/с, α = 30°.
         * {@code s = 200² · sin(60°) / 9.80665}.
         */
        private void testKnownValues() {
            double v0 = 200.0, angle = 30.0;
            double expected = (v0 * v0 * Math.sin(Math.toRadians(60.0))) / BallisticData.G_METRIC;
            BallisticSolver solver = new BallisticSolver(v0, angle);
            solver.solve();
            assertAlmostEqual("Відомі значення (v0=200, α=30°)", expected, solver.getData().getRangeMetric());
        }

        /**
         * Тест: узгодженість метричної та англійської систем.
         * Дальність у футах ≈ дальність у метрах × 3.28084 (похибка &lt; 0.5%).
         */
        private void testMetricImperialConsistency() {
            BallisticSolver solver = new BallisticSolver(150.0, 60.0);
            solver.solve();
            double metricInFeet = solver.getData().getRangeMetric() * BallisticData.METERS_TO_FEET;
            double imperial = solver.getData().getRangeImperial();
            double relErr = Math.abs(metricInFeet - imperial) / imperial;
            report("Узгодженість метрична/англійська (< 0.5%)", relErr < 0.005,
                    String.format("відн.похибка=%.6f", relErr));
        }

        /** Тест: звичайні (не-transient) поля зберігаються після серіалізації. */
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
                          && original.getRangeMetric()      == restored.getRangeMetric()
                          && original.getRangeImperial()    == restored.getRangeImperial();
                report("Серіалізація — збереження звичайних полів", ok, "");
            } catch (Exception e) {
                report("Серіалізація — збереження звичайних полів", false, e.getMessage());
            }
        }

        /**
         * Тест: transient-поля мають значення за замовчуванням після десеріалізації.
         * {@code timestamp → 0L}, {@code statusMessage → null}.
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
                report("Transient: timestamp скинуто в 0", restored.getTimestamp() == 0L,
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
            boolean caught1 = false, caught2 = false;
            try { new BallisticData(100.0, -5.0);  } catch (IllegalArgumentException e) { caught1 = true; }
            try { new BallisticData(100.0, 100.0); } catch (IllegalArgumentException e) { caught2 = true; }
            report("Некоректний кут → виняток", caught1 && caught2, "");
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

        /** Порівнює два числа з похибкою {@link #EPSILON}. */
        private void assertAlmostEqual(String name, double expected, double actual) {
            report(name, Math.abs(expected - actual) < EPSILON,
                    String.format("очікувано=%.6f, отримано=%.6f", expected, actual));
        }

        /** Виводить результат тесту. */
        private void report(String name, boolean success, String details) {
            System.out.printf("  %s : %s", success ? "[OK] PASS" : "[FAIL] FAIL", name);
            if (!details.isEmpty()) System.out.printf("  [%s]", details);
            System.out.println();
            if (success) passed++; else failed++;
        }
    }

    /** Ім'я файлу для серіалізації за замовчуванням. */
    private static final String DEFAULT_FILE = "ballistic.ser";

    /**
     * Серіалізує об'єкт {@link BallisticData} у файл.
     *
     * @param data     об'єкт для збереження
     * @param filename ім'я файлу
     * @throws IOException якщо виникла помилка запису
     */
    private static void serialize(BallisticData data, String filename) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
            oos.writeObject(data);
        }
    }

    /**
     * Десеріалізує об'єкт {@link BallisticData} з файлу.
     *
     * @param filename ім'я файлу
     * @return відновлений об'єкт
     * @throws IOException            помилка читання
     * @throws ClassNotFoundException клас не знайдено
     */
    private static BallisticData deserialize(String filename)
            throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
            return (BallisticData) ois.readObject();
        }
    }

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
     * Точка входу: запускає тести, потім інтерактивну демонстрацію.
     *
     * <p>Сценарій:</p>
     * <ol>
     *   <li>Автоматичне тестування ({@link BallisticTest}).</li>
     *   <li>Введення параметрів з консолі.</li>
     *   <li>Обчислення дальності в обох системах мір.</li>
     *   <li>Серіалізація → десеріалізація.</li>
     *   <li>Порівняння transient-полів до та після.</li>
     * </ol>
     *
     * @param args аргументи командного рядка (не використовуються)
     */
    public static void main(String[] args) throws Exception {

        System.setOut(new PrintStream(System.out, true, "UTF-8"));
        System.setErr(new PrintStream(System.err, true, "UTF-8"));

        BallisticTest tester = new BallisticTest();
        boolean allPassed = tester.runAll();

        if (!allPassed) {
            System.err.println("\n[!] Деякі тести провалені.");
        }

        Scanner scanner = new Scanner(System.in);

        System.out.println("\n+==========================================================+");
        System.out.println("|  Демонстрація серіалізації — Балістична траєкторії ракети  |");
        System.out.println("+============================================================+");

        double velocity = readDouble(scanner, "\n> Введіть початкову швидкість v0 (м/с): ");
        double angle    = readDouble(scanner, "> Введіть кут α до горизонту (градуси, 0–90): ");

        BallisticData data = new BallisticData(velocity, angle);
        BallisticSolver solver = new BallisticSolver(data);
        solver.solve();

        System.out.println("\n" + solver.getReport());

        System.out.println("\n+--- Стан об'єкта ДО серіалізації -------------------+");
        System.out.println(data);
        System.out.println("+----------------------------------------------------+");
        System.out.printf("  > timestamp (transient)     = %d  (ненульовий)%n", data.getTimestamp());
        System.out.printf("  > statusMessage (transient) = \"%s\"%n", data.getStatusMessage());

        try {
            serialize(data, DEFAULT_FILE);
            System.out.printf("%n[OK] Об'єкт серіалізовано у файл \"%s\"%n", DEFAULT_FILE);
        } catch (IOException e) {
            System.err.println("[FAIL] Помилка серіалізації: " + e.getMessage());
            return;
        }

        BallisticData restored;
        try {
            restored = deserialize(DEFAULT_FILE);
            System.out.printf("[OK] Об'єкт десеріалізовано з файлу \"%s\"%n%n", DEFAULT_FILE);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("[FAIL] Помилка десеріалізації: " + e.getMessage());
            return;
        }

        System.out.println("+--- Стан об'єкта ПІСЛЯ десеріалізації --------------+");
        System.out.println(restored);
        System.out.println("+----------------------------------------------------+");

        System.out.println("+--- Порівняння transient-полів ---------------------+");
        System.out.printf("  timestamp   ДО:    %d%n", data.getTimestamp());
        System.out.printf("  timestamp   ПІСЛЯ:  %d  <-- скинуто в 0!%n", restored.getTimestamp());
        System.out.printf("  status      ДО:    \"%s\"%n", data.getStatusMessage());
        System.out.printf("  status      ПІСЛЯ:  %s  <-- скинуто в null!%n", restored.getStatusMessage());
        System.out.println("+----------------------------------------------------+");

        System.out.println("\n+--- Перевірка збережених полів ----------------------+");
        System.out.printf("  v0       : %.2f → %.2f  %s%n",
                data.getInitialVelocity(), restored.getInitialVelocity(),
                data.getInitialVelocity() == restored.getInitialVelocity() ? "[OK]" : "[FAIL]");
        System.out.printf("  angle    : %.2f → %.2f  %s%n",
                data.getAngleDegrees(), restored.getAngleDegrees(),
                data.getAngleDegrees() == restored.getAngleDegrees() ? "[OK]" : "[FAIL]");
        System.out.printf("  metric   : %.4f → %.4f  %s%n",
                data.getRangeMetric(), restored.getRangeMetric(),
                data.getRangeMetric() == restored.getRangeMetric() ? "[OK]" : "[FAIL]");
        System.out.printf("  imperial : %.4f → %.4f  %s%n",
                data.getRangeImperial(), restored.getRangeImperial(),
                data.getRangeImperial() == restored.getRangeImperial() ? "[OK]" : "[FAIL]");
        System.out.println("+----------------------------------------------------+");

        new File(DEFAULT_FILE).delete();
        scanner.close();
        System.out.println("\nДемонстрацію завершено. Ракета полетіла");
    }
}