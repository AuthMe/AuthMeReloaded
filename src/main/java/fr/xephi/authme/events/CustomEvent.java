package fr.xephi.authme.events;

import org.bukkit.event.Event;

/**
 * The parent of all AuthMe events.
 */
public abstract class CustomEvent extends Event {

    /**
     * Constructor.
     */
    public CustomEvent() {
        super(false);
    }

    /**
     * Constructor, specifying whether the event is asynchronous or not.
     *
     * @param isAsync {@code true} to fire the event asynchronously, false otherwise
     * @see Event#Event(boolean)
     */
    public CustomEvent(boolean isAsync) {
        super(isAsync);
    }

}
