package fr.xephi.authme.process.quit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.settings.Settings;

import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

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

    protected void sendBungeeMessage() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF("logout;" + player.getName().toLowerCase());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
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
        if (!Settings.isSessionsEnabled && Settings.bungee)
        	sendBungeeMessage();
    }
}
