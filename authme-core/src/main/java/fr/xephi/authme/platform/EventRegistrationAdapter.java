package fr.xephi.authme.platform;

import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.List;

/**
 * Supplies version-specific listeners to register at startup, in addition to the core set.
 */
public interface EventRegistrationAdapter {

    default List<Class<? extends Listener>> getAdditionalListeners() {
        return Collections.emptyList();
    }
}
