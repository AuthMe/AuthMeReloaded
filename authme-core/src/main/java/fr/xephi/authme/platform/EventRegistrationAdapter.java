package fr.xephi.authme.platform;

import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.listener.EntityListener;
import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.listener.ServerListener;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Supplies the full listener set to register at startup for the active platform.
 */
public interface EventRegistrationAdapter {

    /**
     * Returns the full ordered list of listeners to register for this platform.
     */
    List<Class<? extends Listener>> getListeners();

    /**
     * Replaces the UUID on the given pre-login event with {@code offlineUuid}.
     *
     * <p>This is a platform capability: Paper/Folia implement it via
     * {@code PlayerProfile.setId()} so the player's session UUID is the offline UUID from
     * the start, making it consistent with all other plugins. Spigot provides no stable
     * API to change the UUID at this stage and therefore uses the default no-op.
     */
    default void normalizePreLoginUuid(AsyncPlayerPreLoginEvent event, UUID offlineUuid) {
        // No-op: Spigot does not expose a stable API to change the pre-login UUID
    }

    /**
     * Returns the core listeners shared by all platforms.
     */
    static List<Class<? extends Listener>> getCommonListeners() {
        return List.of(
            PlayerListener.class,
            BlockListener.class,
            EntityListener.class,
            ServerListener.class);
    }

    @SafeVarargs
    static List<Class<? extends Listener>> combineListeners(List<Class<? extends Listener>>... listenerGroups) {
        List<Class<? extends Listener>> listeners = new ArrayList<>();
        for (List<Class<? extends Listener>> listenerGroup : listenerGroups) {
            listeners.addAll(listenerGroup);
        }
        return List.copyOf(listeners);
    }
}
