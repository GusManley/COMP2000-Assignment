import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.DoubleSupplier;
import java.util.stream.IntStream;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JCheckBox;

public class SimulationPanel extends JPanel implements ActionListener {
    private final List<Cell> cells = new ArrayList<>();
    private final List<SimulationStats> statsHistory = new ArrayList<>();
    private SeasonManager seasonManager;
    private long lastUpdateTime;
    private JCheckBox seasonsCheckBox;
    private Timer timer;
    public int simScreenX;
    public int simScreenY;
    public int offset;
    public int statScreenX = 200;
    private boolean cellsCreated = false;
    private JButton resetButton;
    private boolean buttonCreated = false;
    private JPanel settingsPanel;
    private boolean settingsCreated = false;
    private int cellCount = Settings.CELL_COUNT;
    private int deadCellCount;
    private int duplicateCellCount;
    private int simTick = 0;
    private float r0 = 0;
    public BerserkState Berserk;
    private final OutbreakEvents events = new OutbreakEvents();
    private final EventFeed eventFeed = new EventFeed();

    private static final int FORECAST_TRIALS = 64;
    private static final int FORECAST_MAX_TICKS = 2000;
    private JButton forecastButton;
    // Only read and written on the Swing thread (the worker hands results over with invokeLater).
    private OutbreakForecast.Forecast forecast;
    private String forecastError;

    public SimulationPanel() {
        this.setPreferredSize(new Dimension(1200, 1200));
        this.setLayout(null);
        this.setBackground(Color.BLACK);
        deadCellCount = 0;
        duplicateCellCount = 0;
        offset = 10;
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateDimensions();
            }
        });

        // Observers: the counters only care about births and deaths, so this anonymous
        // listener overrides just those two methods. The feed listens for outbreak events.
        events.subscribe(new OutbreakListener() {
            @Override
            public void cellDied(Cell cell) {
                deadCellCount++;
            }

            @Override
            public void cellBorn(Cell parent, Cell child) {
                duplicateCellCount++;
            }
        });
        events.subscribe(eventFeed);

        seasonManager = new SeasonManager(events);

        lastUpdateTime = System.currentTimeMillis();

        timer = new Timer(16, this);
        timer.start();

        Berserk = new BerserkState(); // Create a new Berserk state for Infected cell
    }

    private void createResetButton() {
        resetButton = new JButton("Reset");
        resetButton.addActionListener((ActionEvent e) -> {
            resetSimulation();
        });
        this.add(resetButton);
    }

    private void createForecastButton() {
        forecastButton = new JButton("Forecast");
        forecastButton.setToolTipText("Run " + FORECAST_TRIALS + " simulations from the current settings");
        forecastButton.addActionListener(e -> startForecast());
        this.add(forecastButton);
    }

    /**
     * Runs the forecast on background threads so the animation keeps going.
     *
     * The settings are copied into an immutable Scenario here, on the Swing thread, before
     * any worker starts. The result comes back to the Swing thread through invokeLater,
     * because Swing components and the fields paintComponent reads must only be touched
     * on the Swing thread.
     */
    private void startForecast() {
        OutbreakForecast.Scenario scenario = OutbreakForecast.Scenario.fromSettings(simScreenX, simScreenY);
        forecastButton.setEnabled(false);
        forecastButton.setText("Running...");

        CompletableFuture
                .supplyAsync(() -> OutbreakForecast.run(scenario, FORECAST_TRIALS, FORECAST_MAX_TICKS, true))
                .whenComplete((result, error) -> SwingUtilities.invokeLater(() -> {
                    if (error != null) {
                        forecastError = "Forecast failed: " + error.getMessage();
                    } else {
                        forecast = result;
                        forecastError = null;
                    }
                    forecastButton.setEnabled(true);
                    forecastButton.setText("Forecast");
                }));
    }

    public void resetSimulation() {
        cellCount = Settings.CELL_COUNT;
        cells.clear();
        statsHistory.clear();
        cellsCreated = false;
        deadCellCount = 0;
        duplicateCellCount = 0;
        simTick = 0;
        r0 = 0;
        seasonManager.reset();
        eventFeed.clear();
        lastUpdateTime = System.currentTimeMillis();
        Berserk = new BerserkState(); // create new berserk state after every reset
        updateDimensions();
        repaint();
    }

    private void createSeasonsCheckBox() {
        seasonsCheckBox = new JCheckBox("Enable Seasons", true);
        seasonsCheckBox.setForeground(Color.white);

        seasonsCheckBox.addActionListener(e -> {
            seasonManager.setEnabled(seasonsCheckBox.isSelected());
            repaint();
        });

        this.add(seasonsCheckBox);
    }

    public void updateDimensions() {
        simScreenX = this.getWidth() - statScreenX - offset;
        simScreenY = this.getHeight() - offset;

        if (!cellsCreated && simScreenX > 0 && simScreenY > 0) {
            createCells();
            cellsCreated = true;
        }

        if (!buttonCreated && simScreenX > 0 && simScreenY > 0) {
            createResetButton();
            createForecastButton();
            buttonCreated = true;
        }

        if (!settingsCreated && simScreenX > 0 && simScreenY > 0) {
            settingsPanel = new Settings().settingsPanel();
            settingsPanel.setBounds(simScreenX + offset, offset * 29, 180, 120);
            this.add(settingsPanel);
            settingsPanel.revalidate();
            settingsPanel.repaint();
            settingsCreated = true;
        }

        // Create Seasonal Checkbox once dimensions are available
        if (seasonsCheckBox == null && simScreenX > 0 && simScreenY > 0) {
            createSeasonsCheckBox();
        }
        // Reposition button based on window size
        if (resetButton != null) {
            resetButton.setBounds(simScreenX + offset, offset * 43, 88, 40);
        }
        if (forecastButton != null) {
            forecastButton.setBounds(simScreenX + offset + 92, offset * 43, 88, 40);
        }

        if (settingsPanel != null) {
            settingsPanel.setBounds(simScreenX + offset, offset * 29, 180, 120);
            settingsPanel.revalidate();
            settingsPanel.repaint();
        }

        // Reposition Checkbox based on window size
        if (seasonsCheckBox != null) {
            seasonsCheckBox.setBounds(simScreenX + offset, offset * 39, 170, 25);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawSimBorder(g);
        drawStats(g);

        for (Cell cell : cells) {
            cell.draw(g);
        }
    }

    public void createCells() {
        try {
            checkStartingCounts();
        } catch (InvalidSettingsException e) {
            Settings.INFECTED_COUNT = 3;
            Settings.ANTIVIRUS_COUNT = 3;
        }
        // Suppliers: each call gives a new random position inside the borders.
        DoubleSupplier randomX = () -> offset + Math.random() * (simScreenX - 2 * offset - 20);
        DoubleSupplier randomY = () -> offset + Math.random() * (simScreenY - 2 * offset - 20);

        IntStream.range(0, cellCount)
                .mapToObj(i -> new Cell(randomX.getAsDouble(), randomY.getAsDouble(), i, startingState(i)))
                .forEach(cells::add);
    }

    /** The first cells are infected, the next are antivirus, and the rest are neutral. */
    private static CellState startingState(int index) {
        if (index < Settings.INFECTED_COUNT) {
            return InfectedState.INSTANCE;
        } else if (index < Settings.INFECTED_COUNT + Settings.ANTIVIRUS_COUNT) {
            return AntivirusState.INSTANCE;
        }
        return NeutralState.INSTANCE;
    }

    public void drawSimBorder(Graphics g) {
        Color simulationBackground = seasonManager.getBackground();
        // Sim screen borders
        g.setColor(simulationBackground);
        g.fillRect(offset, offset, simScreenX - offset, simScreenY - offset);
        g.setColor(Color.BLACK);
        g.drawLine(simScreenX, offset, simScreenX, simScreenY);
        g.drawLine(offset, offset, offset, simScreenY);
        g.drawLine(offset, offset, simScreenX, offset);
        g.drawLine(offset, simScreenY, simScreenX, simScreenY);
        // Sim screen background
        g.setColor(simulationBackground);
        g.fillRect(offset, offset, simScreenX - offset, simScreenY - offset);
    }

    /** A one-pass count of the current population (see CensusCollector). */
    public Census census() {
        return Census.of(cells);
    }

    public void EnterBerserk() {
        if (!cellsCreated || cells.isEmpty()) { // Make sure there are cells before activating berserk
            return;
        }

        int numOfInfected = census().infected();

        if (numOfInfected <= 5 && Berserk.getBerserkChanceLeft() > 0 && !Berserk.isBerserk()) {
            try {
                Berserk.EnterBerserk(); // Throws if berserk is not allowed right now

                // Only reached if EnterBerserk() succeeded, so cells are never
                // marked berserk when the exception says berserk was refused.
                cells.stream()
                        .filter(c -> c.is(InfectedState.INSTANCE))
                        .forEach(c -> c.isBerserk = true);
            } catch (IllegalBerserkException e) {
                System.out.println("Failed to enter Berserk: " + e.getMessage());
            }
        } 
        else if (!Berserk.isBerserk()) {

            // Berserk has ended, so clear it from every cell
            cells.forEach(c -> c.isBerserk = false);
        }
    }

    public void drawStats(Graphics g) {
        Census census = census();
        int neutralCount = census.neutral();
        int infectedCount = census.infected();
        int antivirusCount = census.antivirus();

        g.setFont(new Font("Times New Roman", Font.PLAIN, 30));
        g.setColor(Color.BLACK);
        g.drawString("Season: " + seasonManager.getCurrentSeason(), 10, simScreenY - 35);
        g.setColor(Color.WHITE);
        g.drawString("Next Season: " + seasonManager.getSecondsRemaining() + "s", 10, simScreenY - 10);
        g.drawString("Pandemic", simScreenX + offset, offset * 3);
        g.drawString("Simulator", simScreenX + offset, offset * 6);
        g.setFont(new Font("Times New Roman", Font.PLAIN, 20));
        g.drawString("Total Cells: " + cells.size(), simScreenX + offset, offset * 10);

        g.setColor(NeutralState.INSTANCE.getCellColor());
        g.drawString("Neutral Cells: " + neutralCount, simScreenX + offset, offset * 13);
        g.setColor(InfectedState.INSTANCE.getCellColor());
        g.drawString("Infected Cells: " + infectedCount, simScreenX + offset, offset * 16);
        g.setColor(AntivirusState.INSTANCE.getCellColor());
        g.drawString("Antivirus Cells: " + antivirusCount, simScreenX + offset, offset * 19);
        g.setColor(Color.DARK_GRAY);
        g.drawString("Dead Cells: " + deadCellCount, simScreenX + offset, offset * 22);
        g.setColor(Color.yellow);
        g.drawString("Duplicate Cells: " + duplicateCellCount, simScreenX + offset, offset * 25);
        g.setColor(Color.magenta);
        g.drawString("R0 Value: " + r0, simScreenX + offset, offset * 28);

        drawEvolution(g, census);
        drawForecast(g);
        drawEventFeed(g);
    }

    /** How resistant the virus has become, under the reset button. */
    private void drawEvolution(Graphics g, Census census) {
        int x = simScreenX + offset;
        int y = offset * 50;
        g.setFont(new Font("Times New Roman", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString("Virus evolution", x, y);

        g.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        g.setColor(Color.LIGHT_GRAY);
        List<Integer> levels = census.resistanceLevels();
        g.drawString(String.format("Avg resistance: %.2f", census.averageResistance()), x, y += 18);
        g.drawString("Lv0 " + levels.get(0) + "  Lv1 " + levels.get(1)
                + "  Lv2 " + levels.get(2) + "  Lv3 " + levels.get(3), x, y += 18);
        g.drawString(resistanceTrend(), x, y += 18);
        g.drawString("Immune: " + census.immune() + "  Berserk: " + census.berserk(), x, y += 18);
    }

    /** Average resistance at the first snapshot compared with the latest one. */
    private String resistanceTrend() {
        return statsHistory.stream().findFirst()
                .map(first -> String.format("Trend: %.2f -> %.2f",
                        first.getAverageResistance(),
                        statsHistory.get(statsHistory.size() - 1).getAverageResistance()))
                .orElse("Trend: waiting for data");
    }

    /** The latest forecast result, under the virus evolution block. */
    private void drawForecast(Graphics g) {
        int x = simScreenX + offset;
        int y = offset * 60;
        g.setFont(new Font("Times New Roman", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString("Forecast", x, y);

        g.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        g.setColor(Color.LIGHT_GRAY);
        if (forecastError != null) {
            g.drawString(forecastError, x, y + 18);
        } else if (forecast == null) {
            g.drawString("Press Forecast to run " + FORECAST_TRIALS, x, y + 18);
            g.drawString("simulations from these settings", x, y + 36);
        } else {
            g.drawString(String.format("Wiped out in %.0f%% of %d runs",
                    100 * forecast.eradicationRate(), forecast.trials()), x, y + 18);
            g.drawString(String.format("Avg peak %.0f infected, %.1f s",
                    forecast.meanPeakInfected(), forecast.millis() / 1000.0), x, y + 36);
        }
    }

    /** Lists the latest outbreak events under the forecast block. */
    private void drawEventFeed(Graphics g) {
        int x = simScreenX + offset;
        int y = offset * 66 + 4;
        g.setFont(new Font("Times New Roman", Font.BOLD, 16));
        g.setColor(Color.WHITE);
        g.drawString("Recent events", x, y);

        g.setFont(new Font("Times New Roman", Font.PLAIN, 14));
        g.setColor(Color.LIGHT_GRAY);
        for (String line : eventFeed.recent()) {
            y += 18;
            g.drawString(line, x, y);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        simTick++;
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        // The shared rules (also used by the forecast), with seasons in between.
        OutbreakRules.moveAll(cells, simScreenX, simScreenY, offset);
        OutbreakRules.resolveCollisions(cells, events);
        seasonManager.update(cells, deltaTime);
        OutbreakRules.randomTurnover(cells, events);

        if (simTick % 125 == 0) {
            Census census = census();
            r0 = census.r0();
            statsHistory.add(SimulationStats.from(census, deadCellCount, duplicateCellCount, simTick));

            cells.forEach(Cell::resetInfectionWindow);
        }

        EnterBerserk();

        // Draw next frame.
        repaint();
    }

    public List<SimulationStats> getStatsHistory() {
        return statsHistory;
    }


    @Override
    public void addNotify() {
        super.addNotify();
        javax.swing.SwingUtilities.invokeLater(this::updateDimensions);
    }

    public class InvalidSettingsException extends RuntimeException {
        public InvalidSettingsException(String message) {
            super(message);
        }
    }

    /**
     * Checks the starting populations fit inside the total cell count.
     * Previously named validate(), which accidentally overrode JPanel.validate().
     * Swing calls validate() itself while laying out the window, so the check ran
     * (and could throw) inside Swing's layout pass, and the panel's own children
     * were no longer validated through it.
     */
    private void checkStartingCounts() {
        if (Settings.INFECTED_COUNT + Settings.ANTIVIRUS_COUNT > Settings.CELL_COUNT) {
            throw new InvalidSettingsException("Infected + antivirus exceeds total cell count");
        }
    }
}
