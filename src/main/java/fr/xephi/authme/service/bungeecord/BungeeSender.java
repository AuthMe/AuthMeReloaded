package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

import javax.inject.Inject;

public class BungeeSender implements SettingsDependent {

    private final AuthMe plugin;
    private final BukkitService bukkitService;

    private boolean isEnabled;
    private String destinationServerOnLogin;

    /*
     * Constructor.
     */
    @Inject
    BungeeSender(AuthMe plugin, BukkitService bukkitService, Settings settings) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        this.destinationServerOnLogin = settings.getProperty(HooksSettings.BUNGEECORD_SERVER);

        if (this.isEnabled) {
            Messenger messenger = plugin.getServer().getMessenger();
            if (!messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
                messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
            }
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
        bukkitService.sendBungeeMessage(out.toByteArray());
    }

    /**
     * Send a player to a specified server. If no server is configured, this will
     * do nothing.
     *
     * @param player The player to send.
     */
    public void connectPlayerOnLogin(Player player) {
        if (isEnabled && !destinationServerOnLogin.isEmpty()) {
            bukkitService.scheduleSyncDelayedTask(() ->
                sendBungeecordMessage("ConnectOther", player.getName(), destinationServerOnLogin), 5L);
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
            if(!plugin.isEnabled()) {
                ConsoleLogger.debug("Tried to send a " + type + " bungeecord message but the plugin was disabled!");
                return;
            }
            sendBungeecordMessage("AuthMe", type, playerName.toLowerCase());
        }
    }

}
