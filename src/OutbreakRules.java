import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The per-tick rules shared by the live simulation (SimulationPanel) and the forecast
 * (OutbreakForecast), so both run exactly the same rules.
 *
 * Every method only touches the list and events it is given, so separate simulations can
 * run these at the same time on different threads, as long as they don't share a list.
 */
public final class OutbreakRules {
    private static final double DEATH_CHANCE = 0.0001;      // per cell, per tick
    private static final double DUPLICATE_CHANCE = 0.0002;  // per infected/antivirus cell, per tick

    private OutbreakRules() {
    }

    public static void moveAll(List<Cell> cells, int maxX, int maxY, int min) {
        cells.forEach(cell -> cell.move(maxX, maxY, min, min));
    }

    /**
     * Checks every pair of cells once and lets colliding cells react.
     *
     * This stays a plain loop on purpose: each collision changes both cells, so the order
     * of the pairs matters and the work can't be split up safely, and a nested IntStream
     * over the pairs would be harder to read than this loop.
     */
    public static void resolveCollisions(List<Cell> cells, OutbreakEvents events) {
        for (int i = 0; i < cells.size(); i++) {
            for (int j = i + 1; j < cells.size(); j++) {
                Cell a = cells.get(i);
                Cell b = cells.get(j);
                if (a.collidesWith(b)) {
                    a.onCollision(b, events);
                }
            }
        }
    }

    /**
     * Random deaths and duplications. Both lists are picked from the population as it is
     * now, before either change is applied (a dying cell can still duplicate).
     */
    public static void randomTurnover(List<Cell> cells, OutbreakEvents events) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        List<Cell> dying = cells.stream()
                .filter(c -> random.nextDouble() < DEATH_CHANCE)
                .toList();
        List<Cell> duplicating = cells.stream()
                .filter(Cell::canDuplicate)
                .filter(c -> random.nextDouble() < DUPLICATE_CHANCE)
                .toList();

        cells.removeAll(dying);
        dying.forEach(events::died);
        duplicating.forEach(parent -> {
            Cell child = parent.duplicate(cells.size());
            cells.add(child);
            events.born(parent, child);
        });
    }
}
