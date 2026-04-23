package fr.xephi.authme.platform;

/**
 * Combines all platform-specific behavior. Implementations are provided by each version
 * module via ServiceLoader and registered as {@link TeleportAdapter}, {@link ChatAdapter},
 * {@link EventRegistrationAdapter}, {@link SchedulingAdapter}, and
 * {@link CommandRegistrationAdapter} in the DI container.
 */
public interface PlatformAdapter extends TeleportAdapter, ChatAdapter, EventRegistrationAdapter,
    DialogAdapter, CommandRegistrationAdapter, SchedulingAdapter {

    /**
     * Returns a short identifier for logging, e.g. "spigot-legacy", "spigot-1.20", "paper-1.21".
     */
    String getPlatformName();

    /**
     * Returns an error message if this platform adapter does not support the current server,
     * or null if the server is compatible.
     *
     * @return the compatibility error message, or null if compatible
     */
    default String getCompatibilityError() {
        return null;
    }
}
