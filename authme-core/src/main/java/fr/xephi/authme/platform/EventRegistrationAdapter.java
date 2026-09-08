package fr.xephi.authme.platform;

import fr.xephi.authme.listener.BlockListener;
import fr.xephi.authme.listener.EntityListener;
import fr.xephi.authme.listener.PlayerListener;
import fr.xephi.authme.listener.ServerListener;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Supplies the full listener set to register at startup for the active platform.
 */
public interface EventRegistrationAdapter {

    /**
     * Returns the full ordered list of listeners to register for this platform.
     */
    List<Class<? extends Listener>> getListeners();

    /**
     * Returns the core listeners shared by all platforms.
     */
    static List<Class<? extends Listener>> getCommonListeners() {
        return Collections.unmodifiableList(Arrays.asList(
            PlayerListener.class,
            BlockListener.class,
            EntityListener.class,
            ServerListener.class));
    }

    @SafeVarargs
    static List<Class<? extends Listener>> combineListeners(List<Class<? extends Listener>>... listenerGroups) {
        List<Class<? extends Listener>> listeners = new ArrayList<>();
        for (List<Class<? extends Listener>> listenerGroup : listenerGroups) {
            listeners.addAll(listenerGroup);
        }
        return Collections.unmodifiableList(new ArrayList<Class<? extends Listener>>(listeners));
    }
}
