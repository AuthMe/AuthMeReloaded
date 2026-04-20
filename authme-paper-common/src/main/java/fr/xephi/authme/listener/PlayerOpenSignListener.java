package fr.xephi.authme.listener;

import io.papermc.paper.event.player.PlayerOpenSignEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Inject;

/**
 * Blocks unauthenticated players from reading signs.
 */
public class PlayerOpenSignListener implements Listener {

    @Inject
    private ListenerService listenerService;

    PlayerOpenSignListener() {
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerOpenSign(PlayerOpenSignEvent event) {
        Player player = event.getPlayer();
        if (listenerService.shouldCancelEvent(player)) {
            event.setCancelled(true);
        }
    }
}
