package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

import javax.inject.Inject;
import java.util.Locale;

public class BungeeSender implements SettingsDependent {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(BungeeSender.class);
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
        Messenger messenger = plugin.getServer().getMessenger();

        if (this.isEnabled && messenger != null) {
            if (!messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
                messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
            }
            if (!messenger.isOutgoingChannelRegistered(plugin, "authme:main")) {
                messenger.registerOutgoingPluginChannel(plugin, "authme:main");
            }
        } else if (messenger != null) {
            if (messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
                messenger.unregisterOutgoingPluginChannel(plugin, "BungeeCord");
            }
            if (messenger.isOutgoingChannelRegistered(plugin, "authme:main")) {
                messenger.unregisterOutgoingPluginChannel(plugin, "authme:main");
            }
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Send a player to a specified server. If no server is configured, this will
     * do nothing.
     *
     * @param player The player to send.
     */
    public void connectPlayerOnLogin(Player player) {
        if (!isEnabled || destinationServerOnLogin.isEmpty()) {
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(destinationServerOnLogin);
        byte[] payload = out.toByteArray();
        // Add a small delay, just in case...
        bukkitService.scheduleSyncDelayedTask(player, () ->
            bukkitService.sendBungeeMessage(player, payload), 20L);
    }

    /**
     * Sends a message to the AuthMe plugin messaging channel, if enabled.
     *
     * @param player     The player related to the message
     * @param type       The message type, See {@link MessageType}
     */
    public void sendAuthMeBungeecordMessage(Player player, MessageType type) {
        if (!isEnabled) {
            return;
        }
        if (!plugin.isEnabled()) {
            logger.debug("Tried to send a " + type + " bungeecord message but the plugin was disabled!");
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(type.getId());
        out.writeUTF(player.getName().toLowerCase(Locale.ROOT));
        bukkitService.sendAuthMePluginMessage(player, out.toByteArray());
    }

}
