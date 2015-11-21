package fr.xephi.authme.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.util.Utils;

/**
 */
public class AuthMePlayerListener18 implements Listener {

    public AuthMe plugin;

    /**
     * Constructor for AuthMePlayerListener18.
     * @param plugin AuthMe
     */
    public AuthMePlayerListener18(AuthMe plugin) {
        this.plugin = plugin;
    }

    /**
     * Method onPlayerInteractAtEntity.
     * @param event PlayerInteractAtEntityEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

}
