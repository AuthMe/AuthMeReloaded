package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.inject.Inject;
import java.util.Optional;

public class BungeeReceiver implements PluginMessageListener, SettingsDependent {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(BungeeReceiver.class);

    private final AuthMe plugin;
    private final BukkitService bukkitService;
    private final ProxySessionManager proxySessionManager;
    private final Management management;
    private final DataSource dataSource;

    private boolean isEnabled;

    @Inject
    BungeeReceiver(AuthMe plugin, BukkitService bukkitService, ProxySessionManager proxySessionManager,
                   Management management, DataSource dataSource, Settings settings) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        this.proxySessionManager = proxySessionManager;
        this.management = management;
        this.dataSource = dataSource;
        reload(settings);
    }

    @Override
    public void reload(final Settings settings) {
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        if (this.isEnabled) {
            this.isEnabled = bukkitService.isBungeeCordConfiguredForSpigot().orElse(false);
        }
        if (this.isEnabled) {
            final Messenger messenger = plugin.getServer().getMessenger();
            if (!messenger.isIncomingChannelRegistered(plugin, "BungeeCord")) {
                messenger.registerIncomingPluginChannel(plugin, "BungeeCord", this);
            }
        }
    }

    /**
     * Processes the given data input and attempts to translate it to a message for the "AuthMe.v2.Broadcast" channel.
     *
     * @param in the input to handle
     */
    private void handleBroadcast(final ByteArrayDataInput in) {
        // Read data byte array
        final short dataLength = in.readShort();
        final byte[] dataBytes = new byte[dataLength];
        in.readFully(dataBytes);
        final ByteArrayDataInput dataIn = ByteStreams.newDataInput(dataBytes);

        // Parse type
        final String typeId = dataIn.readUTF();
        final Optional<MessageType> type = MessageType.fromId(typeId);
        if (!type.isPresent()) {
            logger.debug("Received unsupported forwarded bungeecord message type! ({0})", typeId);
            return;
        }

        // Parse argument
        final String argument;
        try {
            argument = dataIn.readUTF();
        } catch (IllegalStateException e) {
            logger.warning("Received invalid forwarded plugin message of type " + type.get().name()
                + ": argument is missing!");
            return;
        }

        // Handle type
        switch (type.get()) {
            case UNREGISTER:
                dataSource.invalidateCache(argument);
                break;
            case REFRESH_PASSWORD:
            case REFRESH_QUITLOC:
            case REFRESH_EMAIL:
            case REFRESH:
                dataSource.refreshCache(argument);
                break;
            default:
        }
    }

    /**
     * Processes the given data input and attempts to translate it to a message for the "AuthMe.v2" channel.
     *
     * @param in the input to handle
     */
    private void handle(ByteArrayDataInput in) {
        // Parse type
        final String typeId = in.readUTF();
        final Optional<MessageType> type = MessageType.fromId(typeId);
        if (!type.isPresent()) {
            logger.debug("Received unsupported bungeecord message type! ({0})", typeId);
            return;
        }

        // Parse argument
        final String argument;
        try {
            argument = in.readUTF();
        } catch (IllegalStateException e) {
            logger.warning("Received invalid plugin message of type " + type.get().name()
                + ": argument is missing!");
            return;
        }

        // Handle type
        switch (type.get()) {
            case PERFORM_LOGIN:
                performLogin(argument);
                break;
            default:
        }
    }

    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] data) {
        if (!isEnabled) {
            return;
        }

        final ByteArrayDataInput in = ByteStreams.newDataInput(data);

        // Check subchannel
        final String subChannel = in.readUTF();
        if ("AuthMe.v2.Broadcast".equals(subChannel)) {
            handleBroadcast(in);
        } else if ("AuthMe.v2".equals(subChannel)) {
            handle(in);
        }
    }

    private void performLogin(final String name) {
        Player player = bukkitService.getPlayerExact(name);
        if (player != null && player.isOnline()) {
            management.forceLogin(player, true);
            logger.info("The user " + player.getName() + " has been automatically logged in, "
                + "as requested via plugin messaging.");
        } else {
            proxySessionManager.processProxySessionMessage(name);
            logger.info("The user " + name + " should be automatically logged in, "
                + "as requested via plugin messaging but has not been detected, nickname has been"
                + " added to autologin queue.");
        }
    }

}
