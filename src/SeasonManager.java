import java.awt.Color;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Cycles through the seasons and applies the current season's behaviour.
 *
 * Strategy pattern: SeasonManager is the context, and each Season is a strategy. Instead
 * of one class per season, each enum constant is given its behaviour as lambdas or method
 * references (an effect that runs once per second, and a modifier applied to each cell).
 * SeasonManager never checks which season it is; it just runs the current one.
 */
public class SeasonManager {

    /** A season's once-per-second effect on the whole population. */
    @FunctionalInterface
    interface SeasonEffect {
        void apply(List<Cell> cells, Random random, OutbreakEvents events);

        SeasonEffect NONE = (cells, random, events) -> { };
    }

    public enum Season {
        // name    background                   once per second              per-cell modifier
        WINTER(new Color(210, 230, 245), SeasonManager::winterDeaths, cell -> { }),
        SPRING(Color.LIGHT_GRAY,         SeasonManager::springBirths, cell -> { }),
        SUMMER(Color.LIGHT_GRAY,         SeasonEffect.NONE,           SeasonManager::summerSpeed),
        AUTUMN(Color.LIGHT_GRAY,         SeasonEffect.NONE,           SeasonManager::autumnSize);

        private final Color background;
        private final SeasonEffect effect;
        private final Consumer<Cell> modifier;

        Season(Color background, SeasonEffect effect, Consumer<Cell> modifier) {
            this.background = background;
            this.effect = effect;
            this.modifier = modifier;
        }

        /** The season after this one, wrapping from AUTUMN back to WINTER. */
        public Season next() {
            Season[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    // ---- The seasons' behaviour ----

    /** Winter: every second each cell has a 3% chance to die. */
    private static void winterDeaths(List<Cell> cells, Random random, OutbreakEvents events) {
        List<Cell> dying = cells.stream()
                .filter(cell -> random.nextDouble() < 0.03)
                .toList();

        cells.removeAll(dying);
        dying.forEach(events::died);
    }

    /** Spring: every second each neutral cell has a 10% chance to reproduce. */
    private static void springBirths(List<Cell> cells, Random random, OutbreakEvents events) {
        // Pick the parents first. The stream finishes before cells is changed, so we never
        // modify the list while it is being streamed.
        List<Cell> parents = cells.stream()
                .filter(cell -> cell.is(NeutralState.INSTANCE))
                .filter(cell -> random.nextDouble() < 0.10)
                .toList();

        parents.forEach(parent -> {
            double newX = parent.getX() + (random.nextDouble() * 10 - 5);
            double newY = parent.getY() + (random.nextDouble() * 10 - 5);
            Cell child = new Cell(newX, newY, cells.size(), NeutralState.INSTANCE);
            cells.add(child);
            events.born(parent, child);
        });
    }

    /** Summer: infected cells move 1.4x faster. */
    private static void summerSpeed(Cell cell) {
        if (cell.getState() == InfectedState.INSTANCE) {
            cell.setSpeedMultiplier(1.4);
        }
    }

    /** Autumn: antivirus cells are 1.2x larger. */
    private static void autumnSize(Cell cell) {
        if (cell.getState() == AntivirusState.INSTANCE) {
            cell.setSizeMultiplier(1.2);
        }
    }

    // ---- The context ----

    private final OutbreakEvents events;
    private final Random random = new Random();

    private Season currentSeason;
    private boolean enabled = true;

    private long seasonTimer = 0;
    private long effectTimer = 0;
    private int seasonLengthSeconds;

    public SeasonManager(OutbreakEvents events) {
        this.events = events;
        currentSeason = Season.values()[random.nextInt(Season.values().length)];
        seasonLengthSeconds = randomSeasonLength();
    }

    public void update(List<Cell> cells, long deltaTime) {
        if (!enabled) {
            cells.forEach(SeasonManager::resetModifiers);
            return;
        }

        seasonTimer += deltaTime;
        effectTimer += deltaTime;

        // Apply temporary speed and size modifiers.
        updateCellModifiers(cells);

        // Apply the season's effect once every second.
        while (effectTimer >= 1000) {
            effectTimer -= 1000;
            currentSeason.effect.apply(cells, random, events);
        }

        // Change season when the season timer reaches its limit.
        if (seasonTimer >= seasonLengthSeconds * 1000L) {
            currentSeason = currentSeason.next();
            seasonTimer = 0;
            effectTimer = 0;
            seasonLengthSeconds = randomSeasonLength();
            updateCellModifiers(cells); // apply the new season straight away
        }
    }

    private void updateCellModifiers(List<Cell> cells) {
        cells.forEach(cell -> {
            resetModifiers(cell);                 // clear last season's modifier first
            currentSeason.modifier.accept(cell);  // then let this season apply its own
        });
    }

    private static void resetModifiers(Cell cell) {
        cell.setSpeedMultiplier(1.0);
        cell.setSizeMultiplier(1.0);
    }

    /** Random season length between 5 and 10 seconds. */
    private int randomSeasonLength() {
        return random.nextInt(6) + 5;
    }

    /** The colour to paint behind the cells: the season's colour while seasons are on. */
    public Color getBackground() {
        return enabled ? currentSeason.background : Color.LIGHT_GRAY;
    }

    public Season getCurrentSeason() {
        return currentSeason;
    }

    public int getSecondsRemaining() {
        long remainingMilliseconds = (seasonLengthSeconds * 1000L) - seasonTimer;
        return (int) Math.ceil(remainingMilliseconds / 1000.0);
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        seasonTimer = 0;
        effectTimer = 0;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void reset() {
        currentSeason = Season.SPRING;
        seasonTimer = 0;
        effectTimer = 0;
        seasonLengthSeconds = randomSeasonLength();
    }
}
