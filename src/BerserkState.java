public class BerserkState {
    private boolean isBerserk; // Berserk status T / F
    private int enterBerserkChance; // Chances left to enter berserk

    private long berserkStartTime; // start counting berserk state
    private int berserkDuration = 10000; // duration 10 secs (in milliseconds)

    public BerserkState() {
        this.isBerserk = false; // Start no berserk
        this.enterBerserkChance = 5; // Start with 5 chances
    }
    
    /**
     * This method is for making cells go berserk 
     * Automatically count time and decrease berserk chances
     * Throw exceptions when called during invalid condition
     * @throws IllegalBerserkException
     */
    public void EnterBerserk() throws IllegalBerserkException{

        if (this.enterBerserkChance <= 0) {
            throw new IllegalBerserkException("No berserk uses remaining"); // prevent calling  enterBerserk when no Berserk chances left
        }
        if (this.isBerserk) {
            throw new IllegalBerserkException("Already berserk"); // prevent calling enterBerserk when infected cells are already in berserk state
        }

        this.isBerserk = true;
        this.enterBerserkChance -= 1;
        this.berserkStartTime = System.currentTimeMillis();
    }

    /**
     * This function ends berserk
     */
    public void ExitBerserk() {
        this.isBerserk = false;
    }

    public int getBerserkChanceLeft() {
        return enterBerserkChance;
    }

    /**
     * Check if berserk is still true (also count timer since berserk start)
     * @return true if still berserk, false if not
     */
    public boolean isBerserk() {

        // calculate if exceeds timer
        if (isBerserk && System.currentTimeMillis() - berserkStartTime >= berserkDuration) {
            ExitBerserk(); // exit berserk state if exceeds timer
        }
        return isBerserk;
    }
}


// Create new Exception for throwing when enter berserk in incorrect condition
class IllegalBerserkException extends Exception {
    public IllegalBerserkException(String message) {
        super(message);
    }
}