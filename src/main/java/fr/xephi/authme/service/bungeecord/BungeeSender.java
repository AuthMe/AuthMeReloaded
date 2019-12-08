package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

import javax.inject.Inject;

public class BungeeSender implements SettingsDependent {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(BungeeSender.class);
    private final AuthMe plugin;
    private final BukkitService bukkitService;
    private final DataSource dataSource;

    private boolean isEnabled;
    private String destinationServerOnLogin;

    /*
     * Constructor.
     */
    @Inject
    BungeeSender(final AuthMe plugin, final BukkitService bukkitService, final DataSource dataSource,
                 final Settings settings) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        this.dataSource = dataSource;
        reload(settings);
    }

    @Override
    public void reload(final Settings settings) {
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        this.destinationServerOnLogin = settings.getProperty(HooksSettings.BUNGEECORD_SERVER);

        if (this.isEnabled) {
            final Messenger messenger = plugin.getServer().getMessenger();
            if (!messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
                messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
            }
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    private void sendBungeecordMessage(final String... data) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for (final String element : data) {
            out.writeUTF(element);
        }
        bukkitService.sendBungeeMessage(out.toByteArray());
    }

    private void sendBungeecordMessage(Player player, final String... data) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for (final String element : data) {
            out.writeUTF(element);
        }
        bukkitService.sendBungeeMessage(player, out.toByteArray());
    }

    private void sendForwardedBungeecordMessage(final String subChannel, final String... data) {
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ONLINE");
        out.writeUTF(subChannel);
        final ByteArrayDataOutput dataOut = ByteStreams.newDataOutput();
        for (final String element : data) {
            dataOut.writeUTF(element);
        }
        final byte[] dataBytes = dataOut.toByteArray();
        out.writeShort(dataBytes.length);
        out.write(dataBytes);
        bukkitService.sendBungeeMessage(out.toByteArray());
    }

    /**
     * Send a player to a specified server. If no server is configured, this will
     * do nothing.
     *
     * @param player The player to send.
     */
    public void connectPlayerOnLogin(final Player player) {
        if (isEnabled && !destinationServerOnLogin.isEmpty()) {
            bukkitService.scheduleSyncDelayedTask(() ->
                sendBungeecordMessage(player, "Connect", destinationServerOnLogin), 5L);
        }
    }

    /**
     * Sends a message to the AuthMe plugin messaging channel, if enabled.
     *
     * @param type       The message type, See {@link MessageType}
     * @param playerName the player related to the message
     */
    public void sendAuthMeBungeecordMessage(final MessageType type, final String playerName) {
        if (isEnabled) {
            if (!plugin.isEnabled()) {
                logger.debug("Tried to send a " + type + " bungeecord message but the plugin was disabled!");
                return;
            }
            if (type.isRequiresCaching() && !dataSource.isCached()) {
                return;
            }
            if (type.isBroadcast()) {
                sendForwardedBungeecordMessage("AuthMe.v2.Broadcast", type.getId(), playerName.toLowerCase());
            } else {
                sendBungeecordMessage("AuthMe.v2", type.getId(), playerName.toLowerCase());
            }
        }
    }

}
