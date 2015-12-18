package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.util.Utils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;

/**
 */
public class AuthMePlayerListener18 implements Listener {

    public final AuthMe plugin;

    /**
     * Constructor for AuthMePlayerListener18.
     *
     * @param plugin AuthMe
     */
    public AuthMePlayerListener18(AuthMe plugin) {
        this.plugin = plugin;
    }

    // TODO: npc status can be used to bypass security!!!
    /**
     * Method onPlayerInteractAtEntity.
     *
     * @param event PlayerInteractAtEntityEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (Utils.checkAuth(event.getPlayer())) {
            return;
        }

        if (Utils.isNPC(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

}
