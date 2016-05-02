package fr.xephi.authme.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import fr.xephi.authme.AuthMe;

import javax.inject.Inject;

/**
 * Listener of player events for events introduced in Minecraft 1.9.
 */
public class AuthMePlayerListener19 implements Listener {

    @Inject
    private AuthMe plugin;
    
    public AuthMePlayerListener19() { }
    
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        event.setSpawnLocation(plugin.getSpawnLocation(event.getPlayer()));
    }

}
