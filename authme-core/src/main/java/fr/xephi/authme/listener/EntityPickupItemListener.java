package fr.xephi.authme.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPickupItemEvent;

import javax.inject.Inject;

public class EntityPickupItemListener implements Listener {

    @Inject
    private ListenerService listenerService;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player)) {
            return;
        }
        if (listenerService.shouldCancelEvent((Player) event.getEntity())) {
            event.setCancelled(true);
        }
    }
}
