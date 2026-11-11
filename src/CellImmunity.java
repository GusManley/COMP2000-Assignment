import java.util.concurrent.ThreadLocalRandom;

/**
 * Cell immunity
 * After being cured, the cell gains a chance to gain immunity from an infected cell
 */
public class CellImmunity {
    private boolean isImmune = false;
    private int curedCount = 0;
    private int maxImmunityCount = 1; // 
    private int hitCounts = 0;
    private float chanceOfImmunity = 0.70f;
    private boolean canHaveImmunity = false;

    public boolean getImmunityAllowed() {
         return canHaveImmunity;
    }

    public void setIfImmunityAllowed(boolean value) {
        this.canHaveImmunity = value;
    }

    public int getCuredCount() {
        return this.curedCount;
    }

    public void incrementCuredCount() {
        this.curedCount++;
    }
    
    public boolean getImmunity() {
        return this.isImmune;
    }

    public void setImmunity(boolean immunity) {
        this.isImmune = immunity;
    }

    /**
     * Check if the cell is immune
     */
    public void checkIfImmune() {
        if (this.hitCounts <= this.maxImmunityCount) {
            this.isImmune = true;
            this.hitCounts++;
        } else {
            this.isImmune = false;
            this.curedCount = 0;
            this.hitCounts = 0;
        }
    }

    /**
     * Calculate cell immunity
     * @return
     */
    public boolean calculateIfImmune() {
        if (curedCount > 0) {
            return ThreadLocalRandom.current().nextDouble() < chanceOfImmunity;
        } else {
            return false;
        }
    }
}