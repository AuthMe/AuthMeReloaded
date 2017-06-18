package fr.xephi.authme.process.quit;

import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import org.bukkit.entity.Player;

import javax.inject.Inject;


public class ProcessSyncPlayerQuit implements SynchronousProcess {

    @Inject
    private LimboService limboService;

    @Inject
    private CommandManager commandManager;

    /**
     * Processes a player having quit.
     *
     * @param player the player that left
     * @param wasLoggedIn true if the player was logged in when leaving, false otherwise
     */
    public void processSyncQuit(Player player, boolean wasLoggedIn) {
        if (wasLoggedIn) {
            commandManager.runCommandsOnLogout(player);
        } else {
            limboService.restoreData(player);
            player.saveData(); // #1238: Speed is sometimes not restored properly
        }
        player.leaveVehicle();
    }
}
