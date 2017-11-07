package fr.xephi.authme.service.bungeecord;

import ch.jalu.injector.Injector;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

import javax.inject.Inject;

/**
 * Class to manage all BungeeCord related processes.
 */
public class BungeeService implements SettingsDependent {

    private final AuthMe plugin;
    private final BukkitService service;
    private final Injector injector;

    private boolean isEnabled;
    private String destinationServerOnLogin;

    /*
     * Constructor.
     */
    @Inject
    BungeeService(AuthMe plugin, BukkitService service, Settings settings, Injector injector) {
        this.plugin = plugin;
        this.service = service;
        this.injector = injector;
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
            messenger.registerIncomingPluginChannel(plugin, "BungeeCord", injector.getSingleton(BungeeReceiver.class));
        }
    }

    public boolean isEnabled() {
        return isEnabled;
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
     * @param type       The message type, See {@link MessageType}
     * @param playerName the player related to the message
     */
    public void sendAuthMeBungeecordMessage(String type, String playerName) {
        if (isEnabled) {
            sendBungeecordMessage("AuthMe", type, playerName.toLowerCase());
        }
    }

}
