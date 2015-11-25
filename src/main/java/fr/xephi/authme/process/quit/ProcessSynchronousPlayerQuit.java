package fr.xephi.authme.process.quit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.settings.Settings;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;

/**
 */
public class ProcessSynchronousPlayerQuit implements Runnable {

    protected final AuthMe plugin;
    protected final Player player;
    protected final boolean isOp;
    protected final boolean isFlying;
    protected final boolean needToChange;

    /**
     * Constructor for ProcessSynchronousPlayerQuit.
     *
     * @param plugin       AuthMe
     * @param player       Player
     * @param isOp         boolean
     * @param isFlying     boolean
     * @param needToChange boolean
     */
    public ProcessSynchronousPlayerQuit(AuthMe plugin, Player player
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
     *
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
        } catch (Exception ignored) {
        }
    }
}
