import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Font;
// Cell Class
public class Cell {
    private double x;
    private double y;
    private double velX;
    private double velY;
    public double speed = Settings.CELL_SPEED;
    private int size = Settings.CELL_SIZE;

    private double speedMultiplier = 1.0;
    private double sizeMultiplier = 1.0;
    private int id;
    private CellState state; //
    private Color color; // Default color for the cell
    public boolean isDuplicate = false; // Flag to indicate if the cell is duplicated
    public CellImmunity immunity = new CellImmunity(); // Immunity for neutral cells
    public Resistance resistance = new Resistance(); // Resistance level for virus cells
    public boolean isBerserk = false;

    public int infectionsCaused = 0;
    public boolean hasBeenInfected = false;
    public boolean infectedThisWindow = false;

    private boolean collisionEnabled = Settings.hasCollision; // Collision enabled or not

    private int tick = 0;

    public Cell(double startX, double startY, int id, CellState initialState) {
        this.x = startX;
        this.y = startY;
        this.id = id;
        this.state = initialState;
        this.color = initialState.getCellColor(); // Set color based on initial state
        this.velX = ((Math.random() * 3) - 1) * speed;
        this.velY = ((Math.random() * 3) - 1) * speed;;

        if (state.getType().equals("NEUTRAL")) {
            this.immunity.setIfImmunityAllowed(true);
        }

    }

    public Cell(double startX, double startY, int id, CellState initialState, Color color, boolean isDuplicate) {
        this.x = startX;
        this.y = startY;
        this.id = id;
        this.state = initialState;
        this.color = color;
        this.isDuplicate = isDuplicate;
        this.velX = ((Math.random() * 3) - 1) * speed;
        this.velY = ((Math.random() * 3) - 1) * speed;

        if (state.getType().equals("NEUTRAL")) {
            this.immunity.setIfImmunityAllowed(true);
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getId() {
        return id;
    }

    public CellState getState() {
        return state;
    }

    public int getSize() {
        return (int) Math.round(size * sizeMultiplier);
    }

    // Changes the cell's state
    public void changeState(CellState newState) {
        this.state = newState;
    }

    public void draw(Graphics g) { // Drawing loop for the cell
        tick++;
        int currentSize = getSize(); // For size changes

        if (state.getType().equals("INFECTED")) { // "Pulsate" if the cell is infected
            currentSize = (int) (getSize() + Math.sin(tick * 0.1) * 5); // Sin wave for the pulsing
        }

        int offset = (currentSize - size) / 2;
        int drawX = (int) x - offset;
        int drawY = (int) y - offset;

        int centerX = drawX + (currentSize / 2);
        int centerY = drawY + (currentSize / 2);

        // get glow color to determine glow color based on the type
        Color glowColor = null;
        if (state.getType().equals("INFECTED")) {
            glowColor = Color.RED;
        } else if (state.getType().equals("ANTIVIRUS")) {
            glowColor = Color.BLUE;
        } else {
            if (this.immunity.getImmunity() == true) {
                glowColor = new Color(57, 255, 20);
            } else {
                glowColor = Color.GRAY;
            }
        }

        // 2. Draw the glowing border underneath if a match was found

        Graphics2D g2d = (Graphics2D) g.create(); // Create a copy to protect graphics state

        // outer layer
        g2d.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 40));
        g2d.fillOval(drawX - 8, drawY - 8, currentSize + 16, currentSize + 16);

        // middle layer
        g2d.setColor(new Color(glowColor.getRed(), glowColor.getGreen(), glowColor.getBlue(), 100));
        g2d.fillOval(drawX - 4, drawY - 4, currentSize + 8, currentSize + 8);

        // innner layer
        g2d.setColor(glowColor);
        g2d.drawOval(drawX, drawY, currentSize, currentSize);

        // Add spikes to INFECTED cell
        if (state.getType().equals("INFECTED")) {
            Graphics2D g2dSpikes = (Graphics2D) g.create();

            g2dSpikes.setColor(state.getCellColor()); // Match the main cell color

            g2dSpikes.translate(centerX, centerY);

            int spikeCount = 10; // Total number of spikes around the cell
            int spikeLength = currentSize + 20; // How far the spikes stick out past the cell
            int spikeThickness = currentSize / 5; // Thickness of the spikes
            int cornerArc = 15; // How rounded the spike corners are

            if (this.isBerserk) {
                // ------- Drawing Aura-----------
                int auraDiameter = (int) (currentSize + Math.sin(tick * 0.1) * 30);
                int auraX = centerX - (auraDiameter / 2);
                int auraY = centerY - (auraDiameter / 2);

                g2d.setColor(new Color(255, 200, 0, 180));
                g2d.fillOval(auraX, auraY, auraDiameter, auraDiameter);
            }

            for (int i = 0; i < spikeCount; i++) {
                // ---------------- Add spike for Infected cell-----------------
                // Draw a rectangle in the center
                g2dSpikes.fillRoundRect(-spikeLength / 2, -spikeThickness / 2, spikeLength, spikeThickness, cornerArc,
                        cornerArc);

                // Rotate the canvas for the next spike
                g2dSpikes.rotate(Math.toRadians(360.0 / spikeCount));
                
            }

            g2d.dispose();
            g2dSpikes.dispose();
            // ------------------end add spike------------------------------
        } 
        // ---------------------------- Draw cell body------------------------
        g.setColor(state.getCellColor());
        g.fillOval(drawX, drawY, currentSize, currentSize);
        // ---------------------------- end Draw cell body------------------------

        // ------- Draw resistance level------------------
        int resistLevel = resistance.getLevel();
        if (resistLevel > 0 && state.getType().equals("INFECTED")) {

            // Border color based on resistance level
            Color borderColor;
            if (resistLevel >= 3) {
                borderColor = Color.MAGENTA;   // Level 3: fully immune
            } else if (resistLevel == 2) {
                borderColor = Color.ORANGE;    // Level 2: medium
            } else {
                borderColor = Color.WHITE;     // Level 1: low
            }

            // Draw thick colored border
            Graphics2D g2dResist = (Graphics2D) g.create();
            g2dResist.setColor(borderColor);
            g2dResist.setStroke(new BasicStroke(3));
            g2dResist.drawOval(drawX - 2, drawY - 2, currentSize + 4, currentSize + 4);
            g2dResist.dispose();

            // Draw resistance level number
            Graphics2D g2dText = (Graphics2D) g.create();
            g2dText.setColor(Color.WHITE);
            g2dText.setFont(new Font("Arial", Font.BOLD, 14));
            String lvlText = "R" + resistLevel;
            int textWidth = g2dText.getFontMetrics().stringWidth(lvlText);
            g2dText.drawString(lvlText, centerX - textWidth / 2, centerY + 5);
            g2dText.dispose();
        }
        // --------------- end resistance level ---------------------
    }

    /**
     * Changes both cells' states depending on the type of reaction, and reports what
     * happened (infections, cures, resisted cures) to events so listeners can react.
     */
    public void onCollision(Cell opponent, OutbreakEvents events) {
        try {
            if (opponent == null) {
                throw new InvalidArgumentException("Passed opponent argument is null");
            }
            if (events == null) {
                throw new InvalidArgumentException("Passed events argument is null");
            }

            CellState thisOriginState = this.getState();
            CellState oppOriginState = opponent.getState();
            CellState oppNewState = opponent.state.reactWith(thisOriginState);
            CellState thisNewState = this.state.reactWith(oppOriginState);

            // === Resistance check ===
            if (this.state == InfectedState.INSTANCE && thisNewState == NeutralState.INSTANCE) {
                if (this.isBerserk) {
                    thisNewState = InfectedState.INSTANCE;
                    oppNewState = InfectedState.INSTANCE;
                } else if (this.resistance.canResistAntivirus()) {
                    thisNewState = InfectedState.INSTANCE;
                    this.resistance.increaseLevel();
                    events.resisted(this, this.resistance.getLevel());
                }
                this.immunity.incrementCuredCount(); // Increase cured counts
            }
            if (opponent.state == InfectedState.INSTANCE && oppNewState == NeutralState.INSTANCE) {
                if (opponent.isBerserk) {
                    thisNewState = InfectedState.INSTANCE;
                    oppNewState = InfectedState.INSTANCE;
                } else if (opponent.resistance.canResistAntivirus()) {
                    oppNewState = InfectedState.INSTANCE;
                    opponent.resistance.increaseLevel();
                    events.resisted(opponent, opponent.resistance.getLevel());
                }
                opponent.immunity.incrementCuredCount(); // Increase cured counts
            }
            // === End resistance check ===

            // == Start Cell Immunity ==
            // If neutral cell converted to antivirus, remove immunity
            if (this.state == NeutralState.INSTANCE && thisNewState == AntivirusState.INSTANCE) {
                if (this.immunity.getImmunity() == true) {
                    this.immunity.setImmunity(false);
                }
            }

            if (opponent.state == NeutralState.INSTANCE && oppNewState == AntivirusState.INSTANCE) {
                if (opponent.immunity.getImmunity() == true) {
                    opponent.immunity.setImmunity(false);
                }
            }

            // ---------------------------------Check cell immunity----------------------------------
            // Check if cell is immune and change it back to neutral if so
            if (this.state == NeutralState.INSTANCE && thisNewState == InfectedState.INSTANCE) {
                if (this.immunity.getImmunityAllowed()) {

                    boolean calculatedImmunity = this.immunity.calculateIfImmune();
                    if (this.immunity.getImmunity() == false && calculatedImmunity) {
                        this.immunity.checkIfImmune();
                        thisNewState = NeutralState.INSTANCE;
                        // System.out.println("Immune");
                    } else if (opponent.immunity.getImmunity() == true) {
                        this.immunity.checkIfImmune();
                        thisNewState = NeutralState.INSTANCE;
                        // System.out.println("Immune"); // For debugging
                    } else {
                        // System.out.println("Not immune");
                        this.immunity.setImmunity(false);
                    }
                }
            }

            // Check if cell is immune and change it back to neutral if so
            if (opponent.state == NeutralState.INSTANCE && oppNewState == InfectedState.INSTANCE) {
                if (opponent.immunity.getImmunityAllowed()) {
                    // System.out.println("opp Immunity: "+ (this.immunity.getImmunity() == false));
                    boolean calculatedImmunity = opponent.immunity.calculateIfImmune();
                    if (opponent.immunity.getImmunity() == false && calculatedImmunity) {
                        opponent.immunity.checkIfImmune();
                        oppNewState = NeutralState.INSTANCE;
                        // System.out.println("Immune");

                    } else if (opponent.immunity.getImmunity() == true) {
                        opponent.immunity.checkIfImmune();
                        oppNewState = NeutralState.INSTANCE;
                        // System.out.println("Immune");
                    } else {
                        // System.out.println("Not immune");
                        opponent.immunity.setImmunity(false);
                    }
                }
            }
            // ---------------------------------End cell immunity----------------------------------

            if (oppOriginState == NeutralState.INSTANCE && oppNewState == InfectedState.INSTANCE) {
                infectionsCaused++;
                opponent.hasBeenInfected = true;
                opponent.infectedThisWindow = true;
                events.infected(this, opponent);

            } else if (thisOriginState == NeutralState.INSTANCE && thisNewState == InfectedState.INSTANCE) {
                opponent.infectionsCaused++;
                hasBeenInfected = true;
                infectedThisWindow = true;
                events.infected(opponent, this);
            }

            // Report cures (infected -> neutral after all the checks above).
            if (thisOriginState == InfectedState.INSTANCE && thisNewState == NeutralState.INSTANCE) {
                events.cured(this);
            }
            if (oppOriginState == InfectedState.INSTANCE && oppNewState == NeutralState.INSTANCE) {
                events.cured(opponent);
            }

            // Cell state change
            changeState(thisNewState);
            opponent.changeState(oppNewState);
            if (collisionEnabled) {
                bounceOff(opponent);
            }
        } catch (RuntimeException e) {
            System.err.println(e);
        }
    }

    public void bounceOff(Cell opponent) { // Bounce off logic
        double tempVelX = this.velX;
        double tempVelY = this.velY;

        this.velX = opponent.velX;
        this.velY = opponent.velY;

        opponent.velX = tempVelX;
        opponent.velY = tempVelY;

        // Separate cells to prevent sticking
        double dx = opponent.x - this.x;
        double dy = opponent.y - this.y;
        double distance = Math.hypot(dx, dy);

        if (distance < size) {
            double overlap = size - distance;
            double separationX = (dx / distance) * (overlap / 2 + 1);
            double separationY = (dy / distance) * (overlap / 2 + 1);

            this.x -= separationX;
            this.y -= separationY;
            opponent.x += separationX;
            opponent.y += separationY;
        }
    }

    public void move(int panelMaxWidth, int panelMaxHeight, int panelMinWidth, int panelMinHeight) { // Move logic
        if (panelMaxWidth <= 0 || panelMaxHeight <= 0) {
            return;
        }
        x += velX;
        y += velY;

        if (x <= panelMinWidth) {
            x = panelMinWidth;
            velX = -velX;
        } else if (x + getSize() >= panelMaxWidth) {
            x = panelMaxWidth - getSize();
            velX = -velX;
        }

        if (y <= panelMinHeight) {
            y = panelMinHeight;
            velY = -velY;
        } else if (y + getSize() >= panelMaxHeight) {
            y = panelMaxHeight - getSize();
            velY = -velY;
        }
    }

    public boolean collidesWith(Cell other) { // Collision logic
        if (other == null) {
            throw new InvalidArgumentException("Passed other argument is null");
        }

        int radiusA = getSize() / 2;
        int radiusB = other.getSize() / 2;

        double centXA = x + radiusA;
        double centYA = y + radiusA;

        double centXB = other.x + radiusB;
        double centYB = other.y + radiusB;

        double distance = Math.hypot(
                centXB - centXA,
                centYB - centYA);

        return distance <= radiusA + radiusB;
    }

    public void setSpeedMultiplier(double multiplier) {

        double speedChange = multiplier / speedMultiplier;

        velX *= speedChange;
        //System.out.println("velX:"+ velX);

        velY *= speedChange;

        // Speed reduction to address the effect of a speed bug that increases the cell's speed abnormally high
        if (Math.abs(velX) > 2*speed*1.4) {
            //System.out.println("BAD velX after multiplier: " + velX);
            velX = Math.clamp(velX, -2*speed*1.4, 2*speed*1.4);
            
        }
        if (Math.abs(velY) > 2*speed*1.4) {
            //System.out.println("BAD velY after multiplier: " + velY);
            velY = Math.clamp(velY, -2*speed*1.4, 2*speed*1.4);
        }

        speedMultiplier = multiplier;
    }

    public void setSizeMultiplier(double multiplier) {

        sizeMultiplier = multiplier;
    }

    /** True if this cell is currently in the given state. */
    public boolean is(CellState wanted) {
        return state == wanted;
    }

    /** Infected and antivirus cells can duplicate; neutral cells only reproduce in spring. */
    public boolean canDuplicate() {
        return state == InfectedState.INSTANCE || state == AntivirusState.INSTANCE;
    }

    /** A copy of this cell at the same position, drawn with an orange outline. */
    public Cell duplicate(int newId) {
        return new Cell(x, y, newId, state, Color.ORANGE, true);
    }

    /** Starts a new R0 measuring window for this cell. */
    public void resetInfectionWindow() {
        infectionsCaused = 0;
        infectedThisWindow = false;
    }

}

class InvalidArgumentException extends RuntimeException {
    public InvalidArgumentException(String message) {
        super(message);
    }
}