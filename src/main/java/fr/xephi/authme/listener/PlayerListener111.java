package fr.xephi.authme.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityAirChangeEvent;

import javax.inject.Inject;

/**
 * Listener of player events for events introduced in Minecraft 1.11.
 */
public class PlayerListener111 implements Listener {

    @Inject
    private ListenerService listenerService;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerAirChange(EntityAirChangeEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }
}
