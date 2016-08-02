package fr.xephi.authme.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import javax.inject.Inject;

public class BlockListener implements Listener {

    @Inject
    private ListenerService listenerService;

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (listenerService.shouldCancelEvent(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (listenerService.shouldCancelEvent(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

}
