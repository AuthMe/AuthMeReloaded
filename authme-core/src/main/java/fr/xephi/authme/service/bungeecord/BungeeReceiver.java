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
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.ProxyLoginRequestValidator;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.util.UuidUtils;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;


public class BungeeReceiver implements PluginMessageListener, SettingsDependent {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(BungeeReceiver.class);

    private final AuthMe plugin;
    private final BukkitService bukkitService;
    private final ProxySessionManager proxySessionManager;
    private final Management management;
    private final BungeeSender bungeeSender;
    private final DataSource dataSource;
    private final ProxyLoginRequestValidator proxyLoginRequestValidator;

    private static final String AUTHME_CHANNEL = "authme:main";
    private static final long MAX_AGE_MILLIS = 30_000L;

    private boolean isEnabled;
    private String proxySharedSecret;
    private boolean channelRegistered;

    @Inject
    BungeeReceiver(AuthMe plugin, BukkitService bukkitService, ProxySessionManager proxySessionManager,
                   Management management, BungeeSender bungeeSender, DataSource dataSource,
                   ProxyLoginRequestValidator proxyLoginRequestValidator, Settings settings) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        this.proxySessionManager = proxySessionManager;
        this.management = management;
        this.bungeeSender = bungeeSender;
        this.dataSource = dataSource;
        this.proxyLoginRequestValidator = proxyLoginRequestValidator;
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
        this.proxySharedSecret = settings.getProperty(HooksSettings.PROXY_SHARED_SECRET);
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        final Messenger messenger = plugin.getServer().getMessenger();
        if (messenger == null) {
            return;
        }
        // Track our own registration rather than querying the channel: other listeners
        // (e.g. the Paper configuration-phase receiver) may register the same channel.
        if (this.isEnabled && !channelRegistered) {
            messenger.registerIncomingPluginChannel(plugin, AUTHME_CHANNEL, this);
            channelRegistered = true;
        } else if (!this.isEnabled && channelRegistered) {
            messenger.unregisterIncomingPluginChannel(plugin, AUTHME_CHANNEL, this);
            channelRegistered = false;
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
            logger.warning("Received invalid AuthMe plugin message of type " + type.get().name() + ": argument is missing!");
            return;
        }

        if (type.get() == MessageType.PROXY_STARTED) {
            logger.info("Proxy plugin '" + argument + "' has started and registered the authme:main channel");
            final String proxyName = argument;
            bukkitService.runTaskAsynchronously(() -> {
                List<String> premiumNames = dataSource.getPremiumUsernames();
                if (!premiumNames.isEmpty()) {
                    bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() -> {
                        // Re-fetch a carrier at send-time: the original player may have gone offline
                        // during the async DB query.
                        Player freshCarrier = bukkitService.getOnlinePlayers().stream()
                            .findFirst().orElse(null);
                        if (freshCarrier != null) {
                            bungeeSender.sendPremiumList(freshCarrier, premiumNames);
                            logger.info("Sent premium list (" + premiumNames.size() + " player(s)) to proxy '" + proxyName + "'");
                        } else {
                            logger.warning("Cannot send premium list to proxy '" + proxyName
                                + "': no online player available as carrier.");
                        }
                    });
                }
            });
            return;
        }

        if (type.get() == MessageType.PERFORM_LOGIN) {
            VerifiedProxyLogin verified = parseAndVerifyPerformLogin(in, argument);
            if (verified == null) {
                return;
            }
            performLogin(verified.name, verified.verifiedPremiumUuid);
        }
    }

    /**
     * Parses and HMAC-verifies the remainder of a {@code perform.login} message (everything after the
     * player-name argument).
     *
     * @param in the data input positioned right after the player-name argument
     * @param playerName the player-name argument that was already read
     * @return the verified login data, or {@code null} if the message was malformed or failed verification
     */
    private VerifiedProxyLogin parseAndVerifyPerformLogin(ByteArrayDataInput in, String playerName) {
        long timestamp;
        String uuidOrHmac;
        UUID verifiedPremiumUuid = null;
        String hmac;
        try {
            timestamp = in.readLong();
            uuidOrHmac = in.readUTF();
        } catch (IllegalStateException e) {
            logger.warning("Received perform.login without HMAC — update your proxy plugin");
            return null;
        }
        try {
            UUID parsedUuid = UuidUtils.parseUuidSafely(uuidOrHmac);
            if (parsedUuid != null || uuidOrHmac.isEmpty()) {
                verifiedPremiumUuid = parsedUuid;
                hmac = in.readUTF();
            } else {
                hmac = uuidOrHmac;
            }
        } catch (IllegalStateException e) {
            hmac = uuidOrHmac;
        }
        if (!verifyHmac(playerName, timestamp, verifiedPremiumUuid, hmac)) {
            return null;
        }
        return new VerifiedProxyLogin(playerName, verifiedPremiumUuid);
    }

    /**
     * Validates and queues a {@code perform.login} received during Paper/Folia's connection
     * configuration phase, when the player does not yet exist as a {@link Player}. Queuing it in the
     * {@link ProxySessionManager} lets the blocking pre-join login dialog be skipped (and
     * {@code processJoin} auto-login the player) instead of waiting for the post-join
     * {@code perform.login}, which arrives only after the configuration phase has completed.
     *
     * @param data the raw plugin-message payload
     * @return the normalized player name if this was a valid {@code perform.login}, otherwise {@code null}
     */
    public String handleConfigPhasePerformLogin(byte[] data) {
        if (!isEnabled) {
            return null;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String argument;
        try {
            String typeId = in.readUTF();
            if (!MessageType.PERFORM_LOGIN.getId().equals(typeId)) {
                return null;
            }
            argument = in.readUTF();
        } catch (IllegalStateException e) {
            return null;
        }
        VerifiedProxyLogin verified = parseAndVerifyPerformLogin(in, argument);
        if (verified == null) {
            return null;
        }
        proxySessionManager.processProxySessionMessage(verified.name, verified.verifiedPremiumUuid);
        logger.debug("Config-phase perform.login validated and queued for {0}", verified.name);
        return verified.name.toLowerCase(Locale.ROOT);
    }

    private boolean verifyHmac(String playerName, long timestamp, UUID verifiedPremiumUuid, String providedHmac) {
        if (proxySharedSecret.isEmpty()) {
            logger.warning("Rejected perform.login for " + playerName
                + ": Hooks.proxySharedSecret is not configured — set the same secret on all backend servers and the proxy");
            return false;
        }
        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_AGE_MILLIS) {
            logger.warning("Rejected perform.login for " + playerName + ": message has expired");
            return false;
        }
        String expectedHmac = HashUtils.hmacSha256(proxySharedSecret,
            buildPerformLoginPayload(playerName, timestamp, verifiedPremiumUuid));
        if (!HashUtils.isEqual(expectedHmac, providedHmac)) {
            logger.warning("Rejected perform.login for " + playerName + ": invalid HMAC");
            return false;
        }
        return true;
    }

    private String buildPerformLoginPayload(String playerName, long timestamp, UUID verifiedPremiumUuid) {
        return playerName + ":" + timestamp + ":" + (verifiedPremiumUuid == null ? "" : verifiedPremiumUuid);
    }

    private void performLogin(String name, UUID verifiedPremiumUuid) {
        logger.debug("Received perform.login request for {0}", name);
        // Always queue in the proxy session manager so processJoin can consume it even when
        // the player is already online (PlayerJoinEvent fires before ServerSwitchEvent on the
        // proxy, so processJoin may run before perform.login arrives at this backend).
        proxySessionManager.processProxySessionMessage(name, verifiedPremiumUuid);
        Player player = bukkitService.getPlayerExact(name);
        logger.info("performLogin: " + name + " verifiedPremiumUuid=" + verifiedPremiumUuid
            + " playerOnline=" + (player != null && player.isOnline()));
        if (player != null && player.isOnline()) {
            if (verifiedPremiumUuid == null) {
                completeProxyLogin(player);
            } else {
                bukkitService.runTaskAsynchronously(() -> {
                    if (!proxyLoginRequestValidator.validate(player, verifiedPremiumUuid)) {
                        logger.debug("performLogin: validate() rejected/deferred {0}; removing queued login request", name);
                        proxySessionManager.removeLoginRequest(name);
                        return;
                    }
                    logger.info(name + " premium login accepted by validate(); completing proxy login");
                    bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, () -> {
                        if (player.isOnline()) {
                            completeProxyLogin(player);
                        }
                    });
                });
            }
        } else {
            logger.info(name + " is not yet online; queued for auto-login when they connect.");
        }
    }

    private void completeProxyLogin(Player player) {
        // Player is already online: also drive the login directly in case processJoin
        // has already run past the proxy-session check and created a limbo player.
        management.forceLoginFromProxy(player);
        logger.debug("Sending auto-login ACK for {0}", player.getName());
        bungeeSender.sendAuthMeBungeecordMessage(player, MessageType.PERFORM_LOGIN_ACK);
        logger.info(player.getName() + " has been automatically logged in via proxy request.");
    }

    /**
     * Holder for a validated {@code perform.login}: the player name and the proxy-verified premium UUID
     * (or {@code null} if the player is not a verified premium player).
     */
    private static final class VerifiedProxyLogin {

        private final String name;
        private final UUID verifiedPremiumUuid;

        private VerifiedProxyLogin(String name, UUID verifiedPremiumUuid) {
            this.name = name;
            this.verifiedPremiumUuid = verifiedPremiumUuid;
        }
    }

}
