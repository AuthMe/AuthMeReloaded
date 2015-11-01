package fr.xephi.authme.listener;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEditBookEvent;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.util.Utils;

public class AuthMePlayerListener16 implements Listener {

    public AuthMe plugin;

    public AuthMePlayerListener16(AuthMe plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

}
