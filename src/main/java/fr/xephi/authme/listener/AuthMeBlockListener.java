package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.util.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

/**
 */
public class AuthMeBlockListener implements Listener {

    public final AuthMe instance;

    /**
     * Constructor for AuthMeBlockListener.
     *
     * @param instance AuthMe
     */
    public AuthMeBlockListener(AuthMe instance) {
        this.instance = instance;
    }

    /**
     * Method onBlockPlace.
     *
     * @param event BlockPlaceEvent
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (Utils.checkAuth(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    /**
     * Method onBlockBreak.
     *
     * @param event BlockBreakEvent
     */
    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (Utils.checkAuth(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

}
