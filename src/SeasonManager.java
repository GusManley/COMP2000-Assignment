import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class SeasonManager {

    public enum Season {
        WINTER,
        SPRING,
        SUMMER,
        AUTUMN
    }

    private final SimulationPanel simulationPanel;
    private final Random random = new Random();

    private Season currentSeason;
    private boolean enabled = true;

    private long seasonTimer = 0;
    private long effectTimer = 0;

    private int seasonLengthSeconds;


    public SeasonManager(SimulationPanel simulationPanel) {

        this.simulationPanel = simulationPanel;
        currentSeason = Season.values()[
            random.nextInt(Season.values().length)
        ];

        // Choose a random season length between 5 and 10 seconds.
        seasonLengthSeconds = random.nextInt(6) + 5;

        updateBackground();
    }


    public void update(List<Cell> cells, long deltaTime) {
        if (!enabled) {
            resetCellModifiers(cells);
            return;
        }

        seasonTimer += deltaTime;
        effectTimer += deltaTime;


        // Apply temporary speed and size modifiers.
        updateCellModifiers(cells);


        // Apply effects once every second.
        while (effectTimer >= 1000) {

            effectTimer -= 1000;

            applySeasonEffect(cells);
        }


        // Change season when the season timer reaches its limit.
        if (seasonTimer >= seasonLengthSeconds * 1000L) {

            changeSeason(cells);
        }
    }


    private void changeSeason(List<Cell> cells) {

        // Move to the next season.
        switch (currentSeason) {

            case WINTER:
                currentSeason = Season.SPRING;
                break;

            case SPRING:
                currentSeason = Season.SUMMER;
                break;

            case SUMMER:
                currentSeason = Season.AUTUMN;
                break;

            case AUTUMN:
                currentSeason = Season.WINTER;
                break;
        }


        // Reset timers for the new season.
        seasonTimer = 0;
        effectTimer = 0;

        // Choose a new random season length between 5 and 10 seconds.
        seasonLengthSeconds = random.nextInt(6) + 5;


        // Update the background.
        updateBackground();


        // Immediately apply/reset the new season's modifiers.
        updateCellModifiers(cells);
    }


    private void applySeasonEffect(List<Cell> cells) {

        switch (currentSeason) {

            case WINTER:
                winterEffect(cells);
                break;

            case SPRING:
                springEffect(cells);
                break;

            case SUMMER:
                // Summer has no once-per-second effect.
                break;

            case AUTUMN:
                // Autumn has no once-per-second effect.
                break;
        }
    }


    private void winterEffect(List<Cell> cells) {

        Iterator<Cell> iterator = cells.iterator();

        while (iterator.hasNext()) {

            iterator.next();

            // 3% chance for this cell to die.
            if (random.nextDouble() < 0.03) {

                iterator.remove();

                simulationPanel.incrementDeadCellCount();
            }
        }
    }


    private void springEffect(List<Cell> cells) {

        List<Cell> newCells = new ArrayList<>();

        for (Cell cell : cells) {

            // Only neutral cells can reproduce.
            if (cell.getState().equals(NeutralState.INSTANCE)) {

                // 10% chance for this cell to reproduce.
                if (random.nextDouble() < 0.10) {

                    double newX = cell.getX() + (random.nextDouble() * 10 - 5);
                    double newY = cell.getY() + (random.nextDouble() * 10 - 5);

                    newCells.add(
                        new Cell(
                            newX,
                            newY,
                            cells.size() + newCells.size(),
                            NeutralState.INSTANCE
                        )
                    );

                    simulationPanel.incrementDuplicateCellCount();
                }
            }
        }

        // Add all new cells after the loop has finished.
        cells.addAll(newCells);
    }


    private void updateCellModifiers(List<Cell> cells) {

        for (Cell cell : cells) {

            // Reset modifiers first.
            cell.setSpeedMultiplier(1.0);
            cell.setSizeMultiplier(1.0);
            


            if (currentSeason == Season.SUMMER) {

                // Infected cells move 1.4x faster.
                if (cell.getState().equals(InfectedState.INSTANCE)) {

                    cell.setSpeedMultiplier(1.4);
                }
            }


            if (currentSeason == Season.AUTUMN) {

                // Antivirus cells become 1.2x larger.
                if (cell.getState().equals(AntivirusState.INSTANCE)) {

                    cell.setSizeMultiplier(1.2);
                }
            }
        }
    }
    
    private void resetCellModifiers(List<Cell> cells) {
        for (Cell cell : cells) {
            cell.setSpeedMultiplier(1.0);
            cell.setSizeMultiplier(1.0);
        }
    }


    private void updateBackground() {

        if (enabled && currentSeason == Season.WINTER) {

            simulationPanel.setSimulationBackground(
                new Color(210, 230, 245)
            );

        } else {

            // Return to the normal background outside winter.
            simulationPanel.setSimulationBackground(
                Color.LIGHT_GRAY
            );
        }
    }


    public Season getCurrentSeason() {

        return currentSeason;
    }


    public int getSecondsRemaining() {

        long remainingMilliseconds =
            (seasonLengthSeconds * 1000L) - seasonTimer;

        return (int) Math.ceil(
            remainingMilliseconds / 1000.0
        );
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;

        seasonTimer = 0;
        effectTimer = 0;

        updateBackground();
    }

    public boolean isEnabled() {
         return enabled;
    } 


    public void reset() {

        currentSeason = Season.SPRING;

        seasonTimer = 0;
        effectTimer = 0;

        seasonLengthSeconds = random.nextInt(6) + 5;

        updateBackground();
    }
}