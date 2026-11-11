import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Observer pattern: the subject.
 *
 * The simulation reports what happens here, and this class passes each event on to every
 * subscribed listener. The code that causes an event (a collision, a season, a random
 * death) doesn't need to know who is listening, so new reactions can be added by
 * subscribing another listener instead of editing that code.
 */
public class OutbreakEvents {
    private final List<OutbreakListener> listeners = new ArrayList<>();

    public void subscribe(OutbreakListener listener) {
        listeners.add(Objects.requireNonNull(listener, "listener"));
    }

    public void unsubscribe(OutbreakListener listener) {
        listeners.remove(listener);
    }

    public void infected(Cell carrier, Cell victim) {
        listeners.forEach(l -> l.cellInfected(carrier, victim));
    }

    public void cured(Cell cell) {
        listeners.forEach(l -> l.cellCured(cell));
    }

    public void resisted(Cell cell, int newLevel) {
        listeners.forEach(l -> l.cureResisted(cell, newLevel));
    }

    public void died(Cell cell) {
        listeners.forEach(l -> l.cellDied(cell));
    }

    public void born(Cell parent, Cell child) {
        listeners.forEach(l -> l.cellBorn(parent, child));
    }
}
