package fr.xephi.authme.process.quit;

import fr.xephi.authme.AuthMe;
import org.bukkit.entity.Player;

/**
 */
public class ProcessSyncronousPlayerQuit implements Runnable {

    protected final AuthMe plugin;
    protected final Player player;
    protected final boolean isOp;
    protected final boolean needToChange;

    /**
     * Constructor for ProcessSyncronousPlayerQuit.
     *
     * @param plugin       AuthMe
     * @param player       Player
     * @param isOp         boolean
     * @param needToChange boolean
     */
    public ProcessSyncronousPlayerQuit(AuthMe plugin, Player player
        , boolean isOp, boolean needToChange) {
        this.plugin = plugin;
        this.player = player;
        this.isOp = isOp;
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
        }
        try {
            player.getVehicle().eject();
        } catch (Exception ignored) {
        }
    }
}
