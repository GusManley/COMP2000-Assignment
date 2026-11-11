import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

/**
 * Outbreak forecast: runs many independent copies of the simulation (trials) from the
 * current settings, in parallel, and reports how often the virus was wiped out.
 *
 * Why this is safe to run in parallel:
 *  - Each trial builds its own cells, its own list and its own OutbreakEvents. Nothing a
 *    trial changes is visible to any other trial.
 *  - The only thing the trials share is the Scenario, a record copied from Settings on the
 *    Swing thread before the forecast starts. Records are immutable, so sharing is safe,
 *    and trials never read Settings, which the sliders can change at any moment.
 *  - The state singletons (NeutralState.INSTANCE etc.) have no fields that change.
 *  - Randomness comes from ThreadLocalRandom, so threads don't fight over one generator.
 *  - Results are gathered with toList(), which the stream library makes thread-safe.
 *
 * Simplifications: trials skip seasons and berserk. Berserk times itself with the wall
 * clock, which doesn't fit a run that goes much faster than real time.
 */
public final class OutbreakForecast {
    private static final int OFFSET = 10;

    private OutbreakForecast() {
    }

    /** The settings a forecast starts from. Immutable, so every worker thread can share it. */
    public record Scenario(int cellCount, int infected, int antivirus,
                           int cellSize, double speed, boolean collision,
                           int width, int height) {

        /** Copies the current Settings. Call this on the Swing thread. */
        public static Scenario fromSettings(int width, int height) {
            int infected = Settings.INFECTED_COUNT;
            int antivirus = Settings.ANTIVIRUS_COUNT;
            if (infected + antivirus > Settings.CELL_COUNT) { // same fallback as createCells()
                infected = 3;
                antivirus = 3;
            }
            return new Scenario(Settings.CELL_COUNT, infected, antivirus,
                    Settings.CELL_SIZE, Settings.CELL_SPEED, Settings.hasCollision, width, height);
        }
    }

    /** How one trial ended. */
    record TrialResult(boolean eradicated, int ticks, int peakInfected) {
    }

    /** The combined result of all trials. */
    public record Forecast(int trials, int eradicated, double meanTicksToEradicate,
                           double meanPeakInfected, long millis, boolean parallel) {

        public double eradicationRate() {
            return trials == 0 ? 0 : (double) eradicated / trials;
        }
    }

    /**
     * Runs the trials and combines their results.
     *
     * @param parallel true to spread the trials over all cores with a parallel stream
     */
    public static Forecast run(Scenario scenario, int trials, int maxTicks, boolean parallel) {
        long start = System.nanoTime();

        IntStream trialNumbers = IntStream.range(0, trials);
        if (parallel) {
            trialNumbers = trialNumbers.parallel();
        }
        List<TrialResult> results = trialNumbers
                .mapToObj(i -> runTrial(scenario, maxTicks))
                .toList();

        long millis = (System.nanoTime() - start) / 1_000_000;
        return summarise(results, millis, parallel);
    }

    /** One complete, independent simulation. Everything it changes is created in here. */
    static TrialResult runTrial(Scenario s, int maxTicks) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        OutbreakEvents events = new OutbreakEvents(); // this trial's own; nobody listens

        List<Cell> cells = new ArrayList<>();
        for (int i = 0; i < s.cellCount(); i++) {
            double x = OFFSET + random.nextDouble() * (s.width() - 2 * OFFSET - 20);
            double y = OFFSET + random.nextDouble() * (s.height() - 2 * OFFSET - 20);
            CellState state = i < s.infected() ? InfectedState.INSTANCE
                    : i < s.infected() + s.antivirus() ? AntivirusState.INSTANCE
                    : NeutralState.INSTANCE;
            cells.add(new Cell(x, y, i, state, s.cellSize(), s.speed(), s.collision()));
        }

        int peakInfected = s.infected();
        for (int tick = 1; tick <= maxTicks; tick++) {
            OutbreakRules.moveAll(cells, s.width(), s.height(), OFFSET);
            OutbreakRules.resolveCollisions(cells, events);
            OutbreakRules.randomTurnover(cells, events);

            int infected = Census.of(cells).infected();
            peakInfected = Math.max(peakInfected, infected);
            if (infected == 0) {
                return new TrialResult(true, tick, peakInfected);
            }
        }
        return new TrialResult(false, maxTicks, peakInfected);
    }

    private static Forecast summarise(List<TrialResult> results, long millis, boolean parallel) {
        int eradicated = (int) results.stream().filter(TrialResult::eradicated).count();
        double meanTicks = results.stream()
                .filter(TrialResult::eradicated)
                .mapToInt(TrialResult::ticks)
                .average().orElse(0);
        double meanPeak = results.stream()
                .mapToInt(TrialResult::peakInfected)
                .average().orElse(0);
        return new Forecast(results.size(), eradicated, meanTicks, meanPeak, millis, parallel);
    }

    /**
     * Times the same forecast sequentially and in parallel, using the default settings.
     * Run with: java OutbreakForecast
     */
    public static void main(String[] args) {
        Scenario scenario = Scenario.fromSettings(970, 790);
        int trials = 64;
        int maxTicks = 2000;

        run(scenario, 8, maxTicks, true); // warm-up so the JIT has compiled the hot code

        for (boolean parallel : new boolean[] { false, true }) {
            Forecast f = run(scenario, trials, maxTicks, parallel);
            System.out.printf("%-10s %d trials in %5d ms, virus wiped out in %.0f%%%n",
                    parallel ? "parallel" : "sequential", f.trials(), f.millis(), 100 * f.eradicationRate());
        }
        System.out.println("Cores available: " + Runtime.getRuntime().availableProcessors());
    }
}
