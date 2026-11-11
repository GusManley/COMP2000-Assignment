import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * A custom Collector that turns a stream of cells into a Census in one pass.
 *
 * It implements the Collector interface directly to show the four parts a collector is
 * made of. Each part is returned as a method reference to the mutable Tally below:
 *   supplier    - make an empty Tally
 *   accumulator - fold one cell into a Tally
 *   combiner    - merge two Tallies (used when a parallel stream splits the work)
 *   finisher    - turn the finished Tally into an immutable Census
 *
 * It is safe on a parallel stream because each thread gets its own Tally from the
 * supplier and only ever writes to that one. Tallies are only joined by the combiner,
 * after each thread has finished, so no two threads ever touch the same Tally.
 */
public class CensusCollector implements Collector<Cell, CensusCollector.Tally, Census> {

    /** Highest resistance level a cell can reach (see Resistance). */
    private static final int MAX_RESISTANCE = 3;

    /** The running totals. Only ever used by one thread at a time. */
    static final class Tally {
        private int neutral;
        private int infected;
        private int antivirus;
        private int immune;
        private int berserk;
        private final int[] levels = new int[MAX_RESISTANCE + 1];
        private int windowCarriers;
        private long windowInfections;

        void add(Cell cell) {
            CellState state = cell.getState();
            if (state == NeutralState.INSTANCE) {
                neutral++;
                if (cell.immunity.getImmunity()) {
                    immune++;
                }
            } else if (state == InfectedState.INSTANCE) {
                infected++;
                levels[Math.min(cell.resistance.getLevel(), MAX_RESISTANCE)]++;
                if (cell.isBerserk) {
                    berserk++;
                }
            } else if (state == AntivirusState.INSTANCE) {
                antivirus++;
            }

            if (cell.infectedThisWindow) {
                windowCarriers++;
                windowInfections += cell.infectionsCaused;
            }
        }

        Tally combine(Tally other) {
            neutral += other.neutral;
            infected += other.infected;
            antivirus += other.antivirus;
            immune += other.immune;
            berserk += other.berserk;
            for (int i = 0; i < levels.length; i++) {
                levels[i] += other.levels[i];
            }
            windowCarriers += other.windowCarriers;
            windowInfections += other.windowInfections;
            return this;
        }

        Census finish() {
            List<Integer> levelCounts = java.util.Arrays.stream(levels).boxed().toList();
            return new Census(neutral, infected, antivirus, immune, berserk,
                    levelCounts, windowCarriers, windowInfections);
        }
    }

    @Override
    public Supplier<Tally> supplier() {
        return Tally::new;
    }

    @Override
    public BiConsumer<Tally, Cell> accumulator() {
        return Tally::add;
    }

    @Override
    public BinaryOperator<Tally> combiner() {
        return Tally::combine;
    }

    @Override
    public Function<Tally, Census> finisher() {
        return Tally::finish;
    }

    @Override
    public Set<Characteristics> characteristics() {
        // The order of cells doesn't change the counts, so the stream is free to split
        // and recombine the cells in any order.
        return EnumSet.of(Characteristics.UNORDERED);
    }
}
