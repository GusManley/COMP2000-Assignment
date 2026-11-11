/**
 * Observer pattern: the observer.
 *
 * Anything that wants to know when something happens in the outbreak implements this
 * interface and subscribes to an OutbreakEvents object. Every method has an empty default,
 * so a listener only overrides the events it actually cares about.
 */
public interface OutbreakListener {

    /** A neutral cell was infected by an infected cell. */
    default void cellInfected(Cell carrier, Cell victim) { }

    /** An infected cell was cured by an antivirus cell. */
    default void cellCured(Cell cell) { }

    /** An infected cell survived a cure and its resistance went up to newLevel. */
    default void cureResisted(Cell cell, int newLevel) { }

    /** A cell died (randomly, or from winter). */
    default void cellDied(Cell cell) { }

    /** A cell duplicated (randomly, or from spring) and produced child. */
    default void cellBorn(Cell parent, Cell child) { }
}
