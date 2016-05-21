package fr.xephi.authme.listener;

/*
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.SpawnLoader;
import javax.inject.Inject;
*/
import org.bukkit.event.Listener;

/**
 * Listener of player events for events introduced in Minecraft 1.9.
 */
public class AuthMePlayerListener19 implements Listener {

    /*
    @Inject
    private SpawnLoader spawnLoader;

    @Inject
    private NewSetting settings;
    */
    
    /* WTF was that? We need to check all the settings before moving the player to the spawn!
     * 
     * TODO: fixme please!
     * 
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerSpawn(PlayerSpawnLocationEvent event) {
        if(settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }
        event.setSpawnLocation(spawnLoader.getSpawnLocation(event.getPlayer()));
    }
    */

}
