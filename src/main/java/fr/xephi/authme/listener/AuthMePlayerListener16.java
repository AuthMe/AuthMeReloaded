package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;

/**
 */
public class AuthMePlayerListener16 implements Listener {

    public final AuthMe plugin;

    /**
     * Constructor for AuthMePlayerListener16.
     *
     * @param plugin AuthMe
     */
    public AuthMePlayerListener16(AuthMe plugin) {
        this.plugin = plugin;
    }

    /**
     * Method onPlayerEditBook.
     *
     * @param event PlayerEditBookEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        if (Utils.checkAuth(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

}
