package fr.xephi.authme.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerSignOpenEvent;

import javax.inject.Inject;

/**
 * Blocks unauthenticated players from reading signs (Spigot 1.20.1+).
 */
public class PlayerSignOpenListener implements Listener {

    @Inject
    private ListenerService listenerService;

    PlayerSignOpenListener() {
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerSignOpen(PlayerSignOpenEvent event) {
        Player player = event.getPlayer();
        if (listenerService.shouldCancelEvent(player)) {
            event.setCancelled(true);
        }
    }
}
