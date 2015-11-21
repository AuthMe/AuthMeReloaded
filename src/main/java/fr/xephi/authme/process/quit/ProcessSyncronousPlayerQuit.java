package fr.xephi.authme.process.quit;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.settings.Settings;

/**
 */
public class ProcessSyncronousPlayerQuit implements Runnable {

    protected AuthMe plugin;
    protected Player player;
    protected boolean isOp;
    protected boolean isFlying;
    protected boolean needToChange;

    /**
     * Constructor for ProcessSyncronousPlayerQuit.
     * @param plugin AuthMe
     * @param player Player
     * @param isOp boolean
     * @param isFlying boolean
     * @param needToChange boolean
     */
    public ProcessSyncronousPlayerQuit(AuthMe plugin, Player player
            , boolean isOp, boolean isFlying
            , boolean needToChange) {
        this.plugin = plugin;
        this.player = player;
        this.isOp = isOp;
        this.isFlying = isFlying;
        this.needToChange = needToChange;
    }

    /**
     * Method run.
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {

        if (needToChange) {
            player.setOp(isOp);
            if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
                player.setAllowFlight(isFlying);
                player.setFlying(isFlying);
            }
        }
        try {
            player.getVehicle().eject();
        } catch (Exception e) {
        }
    }
}
