package fr.xephi.authme.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

/**
 * Listener of player events for events introduced in Minecraft 1.8.
 */
public class AuthMePlayerListener18 implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (ListenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

}
