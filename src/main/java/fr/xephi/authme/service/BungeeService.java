package fr.xephi.authme.service;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

import javax.inject.Inject;

/**
 * Class to manage all BungeeCord related processes.
 */
public class BungeeService implements SettingsDependent {

    private AuthMe plugin;

    private boolean isEnabled;
    private String bungeeServer;

    /*
     * Constructor.
     */
    @Inject
    BungeeService(AuthMe plugin, Settings settings) {
        this.plugin = plugin;
        reload(settings);
    }

    /**
     * Send a player to a specified server. If no server is configured, this will
     * do nothing.
     *
     * @param player The player to send.
     */
    public void connectPlayer(Player player) {
        if (!isEnabled || bungeeServer.isEmpty()) {
            return;
        }

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(bungeeServer);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        this.bungeeServer = settings.getProperty(HooksSettings.BUNGEECORD_SERVER);
        Messenger messenger = plugin.getServer().getMessenger();
        if (!this.isEnabled) {
            return;
        }
        if (!messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
            messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
        }
    }
}
