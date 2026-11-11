import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;

public class Settings {
    public static int CELL_SIZE = 35;
    public static double CELL_SPEED = 2.0;
    public static int CELL_COUNT = 50;
    public static int INFECTED_COUNT = 3;
    public static int ANTIVIRUS_COUNT = 6;
    public static boolean hasCollision = true;

    /**
     * Describes one "label | slider | value" row. Everything that differs between the
     * sliders is data or behaviour passed in as lambdas:
     *
     * @param read  where the slider's starting value comes from (a Supplier of int)
     * @param write what to do with a new slider value (a Consumer of int)
     * @param show  how to display a slider value as text (a Function from int to String)
     */
    private record SliderSpec(String label, int min, int max,
                              IntSupplier read, IntConsumer write, IntFunction<String> show) {
    }

    /** One entry per slider; the panel is built from this list. */
    private static final List<SliderSpec> SLIDERS = List.of(
            new SliderSpec("Size", 5, 50,
                    () -> CELL_SIZE, v -> CELL_SIZE = v, String::valueOf),
            // The speed slider works in tenths, so 20 on the slider is a speed of 2.0.
            new SliderSpec("Speed", 1, 100,
                    () -> (int) (CELL_SPEED * 10), v -> CELL_SPEED = v / 10.0, v -> String.valueOf(v / 10.0)),
            new SliderSpec("Count", 10, 200,
                    () -> CELL_COUNT, v -> CELL_COUNT = v, String::valueOf),
            new SliderSpec("InfCells", 1, 20,
                    () -> INFECTED_COUNT, v -> INFECTED_COUNT = v, String::valueOf),
            new SliderSpec("AntiCells", 1, 20,
                    () -> ANTIVIRUS_COUNT, v -> ANTIVIRUS_COUNT = v, String::valueOf));

    public JPanel settingsPanel() {
        // Create a settings panel with a row per slider, then the collision toggle
        JPanel settingsPanel = new JPanel();
        settingsPanel.setLayout(new GridLayout(SLIDERS.size() + 1, 3, 5, 5));
        settingsPanel.setBackground(Color.DARK_GRAY);
        settingsPanel.setOpaque(true);

        SLIDERS.forEach(spec -> addSliderRow(settingsPanel, spec));

        settingsPanel.add(whiteLabel("Collision"));
        JToggleButton collisionToggle = new JToggleButton(Settings.hasCollision ? "On" : "Off");
        collisionToggle.setBackground(Color.WHITE);
        collisionToggle.addActionListener(e -> {
            Settings.hasCollision = !Settings.hasCollision;
            collisionToggle.setText(Settings.hasCollision ? "On" : "Off");
        });
        settingsPanel.add(collisionToggle);

        settingsPanel.doLayout();

        return settingsPanel;
    }

    private static void addSliderRow(JPanel panel, SliderSpec spec) {
        int start = spec.read().getAsInt();

        JSlider slider = new JSlider(spec.min(), spec.max(), start);
        slider.setBackground(Color.WHITE);
        JLabel valueLabel = whiteLabel(spec.show().apply(start));

        slider.addChangeListener(e -> {
            spec.write().accept(slider.getValue());
            valueLabel.setText(spec.show().apply(slider.getValue()));
        });

        panel.add(whiteLabel(spec.label()));
        panel.add(slider);
        panel.add(valueLabel);
    }

    private static JLabel whiteLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(Color.WHITE);
        return label;
    }
}
