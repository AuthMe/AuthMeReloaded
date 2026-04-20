package fr.xephi.authme.platform;

/**
 * Combines all platform-specific behavior. Implementations are provided by each version
 * module via ServiceLoader and registered as {@link TeleportAdapter}, {@link ChatAdapter},
 * and {@link EventRegistrationAdapter} in the DI container.
 */
public interface PlatformAdapter extends TeleportAdapter, ChatAdapter, EventRegistrationAdapter, DialogAdapter {

    /**
     * Returns a short identifier for logging, e.g. "spigot-legacy", "spigot-1.20", "paper-1.21".
     */
    String getPlatformName();
}
