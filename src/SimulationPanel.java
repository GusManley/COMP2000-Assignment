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
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.JCheckBox;

public class SimulationPanel extends JPanel implements ActionListener {
    private final List<Cell> cells = new ArrayList<>();
    private final List<SimulationStats> statsHistory = new ArrayList<>();
    private SeasonManager seasonManager;
    private long lastUpdateTime;
    private Color simulationBackground = Color.LIGHT_GRAY;
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

        seasonManager = new SeasonManager(this);

        lastUpdateTime = System.currentTimeMillis();

        timer = new Timer(16, this);
        timer.start();

        Berserk = new BerserkState(); // Create a new Berserk state for Infected cell
    }

    private void createResetButton() {
        resetButton = new JButton("Reset Simulation");
        resetButton.addActionListener((ActionEvent e) -> {
            resetSimulation();
        });
        this.add(resetButton);
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
            resetButton.setBounds(simScreenX + offset, offset * 43, 180, 50);
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
        g.setColor(simulationBackground);

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
        for (int i = 0; i < cellCount; i++) {
            double randomX = offset + Math.random() * (simScreenX - 2 * offset - 20);
            double randomY = offset + Math.random() * (simScreenY - 2 * offset - 20);

            if (i < Settings.INFECTED_COUNT) {
                cells.add(new Cell(randomX, randomY, i, InfectedState.INSTANCE));
            } else if (i < Settings.ANTIVIRUS_COUNT + Settings.INFECTED_COUNT) {
                cells.add(new Cell(randomX, randomY, i, AntivirusState.INSTANCE));
            } else {
                cells.add(new Cell(randomX, randomY, i, NeutralState.INSTANCE));
            }
        }
    }

    public void drawSimBorder(Graphics g) {
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

    public int[] countByType() {
        int neutral = 0, infected = 0, antivirus = 0;
        for (Cell c : cells) {
            if (c.getState() == NeutralState.INSTANCE)
                neutral++;
            if (c.getState() == InfectedState.INSTANCE)
                infected++;
            if (c.getState() == AntivirusState.INSTANCE)
                antivirus++;
        }
        return new int[] { neutral, infected, antivirus };
    }

    public void EnterBerserk() {
        if (!cellsCreated || cells.isEmpty()) { // Make sure there are cells before activating berserk
            return;
        }

        int numOfInfected = countByType()[1];

        if (numOfInfected <= 5 && Berserk.getBerserkChanceLeft() > 0 && !Berserk.isBerserk()) {
            try {
                Berserk.EnterBerserk(); // Throws if berserk is not allowed right now

                // Only reached if EnterBerserk() succeeded, so cells are never
                // marked berserk when the exception says berserk was refused.
                for (Cell c : cells) {
                    if (c.getState() == InfectedState.INSTANCE) {
                        c.isBerserk = true;
                    }
                }
            } catch (IllegalBerserkException e) {
                System.out.println("Failed to enter Berserk: " + e.getMessage());
            }
        } 
        else if (!Berserk.isBerserk()) {

            for (Cell c : cells) {
                if (c.isBerserk) {
                    c.isBerserk = false;
                }
            }

            // Turn Berserk state off
        }
    }

    public void drawStats(Graphics g) {
        int[] counts = countByType();
        int neutralCount = counts[0];
        int infectedCount = counts[1];
        int antivirusCount = counts[2];

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
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        simTick++;
        long currentTime = System.currentTimeMillis();
        long deltaTime = currentTime - lastUpdateTime;
        lastUpdateTime = currentTime;

        // Move each cell.
        for (Cell cell : cells) {
            cell.move(simScreenX, simScreenY, offset, offset);
        }
        // Check cell collisions.
        for (int i = 0; i < cells.size(); i++) {

            for (int j = i + 1; j < cells.size(); j++) {

                Cell a = cells.get(i);
                Cell b = cells.get(j);

                if (a.collidesWith(b)) {

                    a.onCollision(b);
                }
            }
        }

        // Update the season system.
        seasonManager.update(cells, deltaTime);

        List<Cell> toRemove = new ArrayList<>();
        List<Cell> toAdd = new ArrayList<>();
        for (Cell c : cells) {
            if (Math.random() < 0.0001) {
                toRemove.add(c);
            }
            if ((c.getState() == InfectedState.INSTANCE || c.getState() == AntivirusState.INSTANCE)
                    && Math.random() < 0.0002) {
                toAdd.add(new Cell(c.getX(), c.getY(), cells.size(), c.getState(), Color.ORANGE, true));
            }
        }

        cells.removeAll(toRemove);
        deadCellCount += toRemove.size();
        cells.addAll(toAdd);
        duplicateCellCount += toAdd.size();

        if (simTick % 125 == 0) {
            r0 = calculateR0();

            int[] counts = countByType();
            statsHistory.add(new SimulationStats(
                    counts[0], counts[1], counts[2],
                    deadCellCount, duplicateCellCount, r0, simTick));

            for (Cell c : cells) {
                c.infectionsCaused = 0;
                c.infectedThisWindow = false;
            }
        }

        EnterBerserk();

        // Draw next frame.
        repaint();
    }

    public float calculateR0() {
        float r0InfectedCount = 0;
        float totalInfectionsCaused = 0;
        for (Cell c : cells) {
            if (c.infectedThisWindow) {
                r0InfectedCount++;
                totalInfectionsCaused += c.infectionsCaused;
            }
        }
        if (r0InfectedCount == 0) {
            return 0;
        }
        return totalInfectionsCaused / r0InfectedCount;
    }

    public List<SimulationStats> getStatsHistory() {
        return statsHistory;
    }

    public void setSimulationBackground(Color background) {
        simulationBackground = background;
        repaint();
    }

    public void incrementDeadCellCount() {

        deadCellCount++;
    }

    public void incrementDuplicateCellCount() {

        duplicateCellCount++;
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
