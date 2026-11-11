import java.util.Collection;
import java.util.List;

/**
 * An immutable head count of the population at one moment, built in a single pass by
 * CensusCollector.
 *
 * @param neutral          cells in each state
 * @param infected
 * @param antivirus
 * @param immune           neutral cells that are currently immune
 * @param berserk          infected cells that are currently berserk
 * @param resistanceLevels how many infected cells are at resistance level 0, 1, 2 and 3
 * @param windowCarriers   cells infected during the current R0 window
 * @param windowInfections infections those cells went on to cause
 */
public record Census(int neutral, int infected, int antivirus, int immune, int berserk,
                     List<Integer> resistanceLevels, int windowCarriers, long windowInfections) {

    /** Counts a group of cells with a sequential stream. */
    public static Census of(Collection<Cell> cells) {
        return cells.stream().collect(new CensusCollector());
    }

    public int total() {
        return neutral + infected + antivirus;
    }

    /** Average resistance level across the infected cells (0 when there are none). */
    public double averageResistance() {
        if (infected == 0) {
            return 0;
        }
        int weighted = 0;
        for (int level = 0; level < resistanceLevels.size(); level++) {
            weighted += level * resistanceLevels.get(level);
        }
        return (double) weighted / infected;
    }

    /** Average infections caused by each cell infected this window. */
    public float r0() {
        return windowCarriers == 0 ? 0f : (float) windowInfections / windowCarriers;
    }
}
