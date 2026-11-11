import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * An OutbreakListener that keeps the most recent outbreak events as short text lines,
 * so the side panel can show what is happening between the cells.
 *
 * It only overrides the three outbreak events. Births and deaths happen every second in
 * winter and spring and would flood the feed, so it leaves those as the empty defaults.
 */
public class EventFeed implements OutbreakListener {
    private static final int MAX_LINES = 8;
    private final Deque<String> lines = new ArrayDeque<>();

    @Override
    public void cellInfected(Cell carrier, Cell victim) {
        int level = carrier.resistance.getLevel();
        add(level > 0 ? "R" + level + " carrier infected a cell" : "Cell infected");
    }

    @Override
    public void cellCured(Cell cell) {
        add("Infected cell cured");
    }

    @Override
    public void cureResisted(Cell cell, int newLevel) {
        add("Cure resisted -> R" + newLevel);
    }

    private void add(String line) {
        lines.addFirst(line);
        if (lines.size() > MAX_LINES) {
            lines.removeLast();
        }
    }

    /** Newest first. */
    public List<String> recent() {
        return List.copyOf(lines);
    }

    public void clear() {
        lines.clear();
    }
}
