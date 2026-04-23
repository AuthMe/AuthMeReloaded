package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.HashUtils;
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
    private final BungeeSender bungeeSender;

    private static final String AUTHME_CHANNEL = "authme:main";
    private static final long MAX_AGE_MILLIS = 30_000L;

    private boolean isEnabled;
    private String proxySharedSecret;

    @Inject
    BungeeReceiver(AuthMe plugin, BukkitService bukkitService, ProxySessionManager proxySessionManager,
                   Management management, BungeeSender bungeeSender, Settings settings) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        this.proxySessionManager = proxySessionManager;
        this.management = management;
        this.bungeeSender = bungeeSender;
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
        this.proxySharedSecret = settings.getProperty(HooksSettings.PROXY_SHARED_SECRET);
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        final Messenger messenger = plugin.getServer().getMessenger();
        if (this.isEnabled && messenger != null) {
            if (!messenger.isIncomingChannelRegistered(plugin, AUTHME_CHANNEL)) {
                messenger.registerIncomingPluginChannel(plugin, AUTHME_CHANNEL, this);
            }
        } else if (messenger != null && messenger.isIncomingChannelRegistered(plugin, AUTHME_CHANNEL)) {
            messenger.unregisterIncomingPluginChannel(plugin, AUTHME_CHANNEL, this);
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!isEnabled || !channel.equals(AUTHME_CHANNEL)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(data);

        String typeId;
        try {
            typeId = in.readUTF();
        } catch (IllegalStateException e) {
            logger.warning("Received malformed AuthMe plugin message on authme:main");
            return;
        }

        Optional<MessageType> type = MessageType.fromId(typeId);
        if (!type.isPresent()) {
            logger.debug("Received unsupported AuthMe plugin message type: {0}", typeId);
            return;
        }

        String argument;
        try {
            argument = in.readUTF();
        } catch (IllegalStateException e) {
            logger.warning("Received invalid AuthMe plugin message of type " + type.get().name()
                + ": argument is missing!");
            return;
        }

        if (type.get() == MessageType.PROXY_STARTED) {
            logger.info("Proxy plugin '" + argument + "' has started and registered the authme:main channel");
            return;
        }

        if (type.get() == MessageType.PERFORM_LOGIN) {
            long timestamp;
            String hmac;
            try {
                timestamp = in.readLong();
                hmac = in.readUTF();
            } catch (IllegalStateException e) {
                logger.warning("Received perform.login without HMAC — update your proxy plugin");
                return;
            }
            if (!verifyHmac(argument, timestamp, hmac)) {
                return;
            }
            performLogin(argument);
        }
    }

    private boolean verifyHmac(String playerName, long timestamp, String providedHmac) {
        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_AGE_MILLIS) {
            logger.warning("Rejected perform.login for " + playerName + ": message has expired");
            return false;
        }
        String expectedHmac = HashUtils.hmacSha256(proxySharedSecret, playerName + ":" + timestamp);
        if (!HashUtils.isEqual(expectedHmac, providedHmac)) {
            logger.warning("Rejected perform.login for " + playerName + ": invalid HMAC");
            return false;
        }
        return true;
    }

    private void performLogin(String name) {
        logger.debug("Received perform.login request for " + name);
        Player player = bukkitService.getPlayerExact(name);
        if (player != null && player.isOnline()) {
            management.forceLoginFromProxy(player);
            logger.debug("Sending auto-login ACK for " + player.getName());
            bungeeSender.sendAuthMeBungeecordMessage(player, MessageType.PERFORM_LOGIN_ACK);
            logger.info(player.getName() + " has been automatically logged in via proxy request.");
        } else {
            proxySessionManager.processProxySessionMessage(name);
            logger.info(name + " is not yet online; queued for auto-login when they connect.");
        }
    }

}
