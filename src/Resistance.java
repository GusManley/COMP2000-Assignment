
/**
 * Resistance system for infected cells.
 * Higher level = higher chance to resist antivirus cure.
 */
public class Resistance {

    private static final int MAX_LEVEL = 3;
    private int level;

    public Resistance() {
        this.level = 0;
    }

    public Resistance(int level) {
        this.level = Math.max(0, Math.min(MAX_LEVEL, level));
    }

    public int getLevel() {
        return level;
    }

    /** Increase resistance level by 1 (max 3). */
    public void increaseLevel() {
        if (level < MAX_LEVEL) level++;
    }

    /** Reset resistance to 0. */
    public void reset() {
        level = 0;
    }

    /**
     * Check if cell can resist antivirus cure.
     * @return true if survives (stays infected), false if cured
     */
    public boolean canResistAntivirus() {
        if (level == 0) return Math.random() < 0.10;  // 10% chance
        if (level == 1) return Math.random() < 0.20;  // 20% chance
        if (level == 2) return Math.random() < 0.25;  // 25% chance
        if (level > 2) return Math.random() < 0.30;  // 30% chance
        return true;                        
    }
}
