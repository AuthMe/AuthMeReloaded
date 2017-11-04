package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.inject.Inject;

/**
 * Class to manage all BungeeCord related processes.
 */
public class BungeeService implements SettingsDependent, PluginMessageListener {

    private final AuthMe plugin;
    private final BukkitService service;
    private final DataSource dataSource;

    private boolean isEnabled;
    private String destinationServerOnLogin;


    /*
     * Constructor.
     */
    @Inject
    BungeeService(AuthMe plugin, BukkitService service, Settings settings, DataSource dataSource) {
        this.plugin = plugin;
        this.service = service;
        this.dataSource = dataSource;
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        this.destinationServerOnLogin = settings.getProperty(HooksSettings.BUNGEECORD_SERVER);
        Messenger messenger = plugin.getServer().getMessenger();
        if (!this.isEnabled) {
            return;
        }
        if (!messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
            messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
        }
        if (!messenger.isIncomingChannelRegistered(plugin, "BungeeCord")) {
            messenger.registerIncomingPluginChannel(plugin, "BungeeCord", this);
        }
    }

    private void sendBungeecordMessage(String... data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for (String element : data) {
            out.writeUTF(element);
        }
        service.sendPluginMessage("BungeeCord", out.toByteArray());
    }

    /**
     * Send a player to a specified server. If no server is configured, this will
     * do nothing.
     *
     * @param player The player to send.
     */
    public void connectPlayerOnLogin(Player player) {
        if (isEnabled && !destinationServerOnLogin.isEmpty()) {
            service.scheduleSyncDelayedTask(() ->
                sendBungeecordMessage("Connect", player.getName(), destinationServerOnLogin), 20L);
        }
    }

    /**
     * Sends a message to the AuthMe plugin messaging channel, if enabled.
     *
     * @param type The message type, See {@link MessageType}
     * @param playerName the player related to the message
     */
    public void sendAuthMeBungeecordMessage(String type, String playerName) {
        if (isEnabled) {
            sendBungeecordMessage("AuthMe", type, playerName.toLowerCase());
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!isEnabled) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String subchannel = in.readUTF();
        if (!"Authme".equals(subchannel)) {
            return;
        }

        String type = in.readUTF();
        String name = in.readUTF();
        switch (type) {
            case MessageType.UNREGISTER:
                dataSource.invalidateCache(name);
                break;
            case MessageType.REFRESH_PASSWORD:
            case MessageType.REFRESH_QUITLOC:
            case MessageType.REFRESH_EMAIL:
            case MessageType.REFRESH:
                dataSource.refreshCache(name);
                break;
            default:
                ConsoleLogger.debug("Received unsupported bungeecord message type! ({0})", type);
        }
    }

}
