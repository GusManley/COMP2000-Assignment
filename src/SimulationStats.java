

public class SimulationStats {

    /** Builds a snapshot from a census plus the counters a census can't see. */
    public static SimulationStats from(Census census, int deadCellCount, int duplicateCellCount, int tick) {
        return new SimulationStats(census.neutral(), census.infected(), census.antivirus(),
                deadCellCount, duplicateCellCount, census.r0(), census.averageResistance(), tick);
    }

    private final int neutralCount;
    private final int infectedCount;
    private final int antivirusCount;
    private final int deadCellCount;
    private final int duplicateCellCount;
    private final float r0Value;
    private final double averageResistance;
    private final int tickSnapshot;

    public SimulationStats(int neutralCount, int infectedCount, int antivirusCount, int deadCellCount, int duplicateCellCount,
        float r0Value, double averageResistance, int tickSnapshot){
        this.neutralCount = neutralCount;
        this.infectedCount = infectedCount;
        this.antivirusCount = antivirusCount;
        this.deadCellCount = deadCellCount;
        this.duplicateCellCount = duplicateCellCount;
        this.r0Value = r0Value;
        this.averageResistance = averageResistance;
        this.tickSnapshot = tickSnapshot;
    }

    public int getNeutralCount(){
        return neutralCount;
    }
    public int getInfectedCount(){
        return infectedCount;
    }
    public int getAntivirusCount(){
        return antivirusCount;
    }
    public int getDeadCellCount(){
        return deadCellCount;
    }
    public int getDuplicateCellCount(){
        return duplicateCellCount;
    }
    public float getR0Value(){
        return r0Value;
    }
    public double getAverageResistance(){
        return averageResistance;
    }
    public int getTickSnapshot(){
        return tickSnapshot;
    }
}
