package fr.xephi.authme.velocity;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.LegacyChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

final class VelocityProxyBridge {

    static final MinecraftChannelIdentifier AUTHME_CHANNEL = MinecraftChannelIdentifier.create("authme", "main");
    static final ChannelIdentifier AUTHME_LEGACY_CHANNEL = new LegacyChannelIdentifier("authme:main");

    private static final String LOGIN_MESSAGE = "login";
    private static final String LOGOUT_MESSAGE = "logout";
    private static final String PERFORM_LOGIN_MESSAGE = "perform.login";
    private static final String PERFORM_LOGIN_ACK_MESSAGE = "perform.login.ack";
    private static final String PROXY_STARTED_MESSAGE = "proxy.started";
    private static final String PREMIUM_SET_MESSAGE = "premium.set";
    private static final String PREMIUM_UNSET_MESSAGE = "premium.unset";
    private static final String PREMIUM_LIST_MESSAGE = "premium.list";
    private static final String PREMIUM_PENDING_SET_MESSAGE = "premium.pending.set";
    private static final String PROXY_IDENTITY = "velocity";
    private static final int MAX_RETRIES = 3;

    private final ProxyServer proxyServer;
    private final Logger logger;
    private VelocityProxyConfiguration configuration;
    private final VelocityAuthenticationStore authenticationStore;
    private final Map<String, AtomicInteger> pendingAutoLogins = new ConcurrentHashMap<>();
    private final Set<String> notifiedAuthServers = ConcurrentHashMap.newKeySet();
    private volatile Set<String> premiumUsernames = ConcurrentHashMap.newKeySet();
    // Players with a pending premium verification (ran /premium but not yet confirmed via reconnect)
    private volatile Set<String> pendingPremiumUsernames = ConcurrentHashMap.newKeySet();
    // Players whose Mojang UUID was confirmed by the proxy during the login phase (LoginSuccess with UUID v4)
    private final Set<String> proxyVerifiedPremium = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "authme-velocity-retry");
        t.setDaemon(true);
        return t;
    });

    VelocityProxyBridge(ProxyServer proxyServer, Logger logger, VelocityProxyConfiguration configuration,
                        VelocityAuthenticationStore authenticationStore) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.configuration = configuration;
        this.authenticationStore = authenticationStore;
    }

    private void markProxyVerifiedPremium(String normalizedName) {
        if (proxyVerifiedPremium.add(normalizedName)) {
            logger.info("Proxy-verified premium: '{}' authenticated online-mode with Mojang", normalizedName);
        }
    }

    /**
     * Records the player as proxy-verified premium when the proxy finished the Mojang authentication
     * phase in online mode. Called from {@code AuthMeVelocityPlugin#onLogin(LoginEvent)}.
     */
    void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        if (!player.isOnlineMode()) {
            return;
        }
        markProxyVerifiedPremium(normalizeName(player.getUsername()));
    }

    /**
     * Fallback hook on {@link PostLoginEvent}: if the player has a UUID v4 (Mojang-issued), they
     * are recorded as proxy-verified premium even if the {@link LoginEvent} listener missed them.
     *
     * <p>This is a secondary signal only — the primary gate is the backend's UUID comparison
     * ({@code verifiedUuid.equals(auth.getPremiumUuid())}), so a fake v4 UUID injected by a
     * third-party plugin would still be rejected there unless it matches the stored premium UUID.</p>
     */
    void onPostLogin(PostLoginEvent event) {
        Player player = event.getPlayer();
        if (player.getUniqueId() != null && player.getUniqueId().version() == 4) {
            markProxyVerifiedPremium(normalizeName(player.getUsername()));
        }
    }

    void reload(VelocityProxyConfiguration configuration) {
        this.configuration = configuration;
        logger.info("Configuration reloaded");
    }

    void registerChannels() {
        proxyServer.getChannelRegistrar().register(AUTHME_CHANNEL, AUTHME_LEGACY_CHANNEL);
        logger.info("Registered AuthMe Velocity bridge channel");
        broadcastProxyStartedHandshake();
    }

    private void broadcastProxyStartedHandshake() {
        byte[] payload = createProxyStartedMessage();
        int notified = 0;
        int deferred = 0;
        for (RegisteredServer server : proxyServer.getAllServers()) {
            if (!configuration.isAuthServer(server)) {
                continue;
            }
            String serverName = server.getServerInfo().getName();
            if (server.sendPluginMessage(AUTHME_CHANNEL, payload)) {
                notifiedAuthServers.add(serverName);
                notified++;
                logger.info("Sent proxy.started handshake to auth server '{}'", serverName);
            } else {
                deferred++;
                logger.info("Deferred proxy.started handshake for '{}' (no connected players yet);"
                    + " will retry on first player connection", serverName);
            }
        }
        if (notified > 0 || deferred > 0) {
            logger.info("Proxy startup handshake: {} auth server(s) notified, {} deferred until first connection",
                notified, deferred);
        }
    }

    private byte[] createProxyStartedMessage() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(PROXY_STARTED_MESSAGE);
        output.writeUTF(PROXY_IDENTITY);
        return output.toByteArray();
    }

    void logConfigurationDetails() {
        if (configuration.allServersAreAuthServers()) {
            logger.info("All registered backend servers are treated as auth servers");
        } else if (configuration.authServers().isEmpty()) {
            logger.warn("No auth servers are configured; autoLogin will only work after authServers is populated"
                + " or allServersAreAuthServers is enabled");
        } else {
            logger.info("Current auth servers:");
            configuration.authServers().forEach(serverName -> logger.info("> {}", serverName));
        }

        if (!configuration.autoLoginEnabled()) {
            logger.info("autoLogin is disabled");
        }

        if (configuration.sendOnLogoutEnabled() && configuration.sendOnLogoutTarget().isEmpty()) {
            logger.warn("sendOnLogout is enabled but unloggedUserServer is empty; logout redirects will be skipped");
        }
    }

    void onPluginMessage(PluginMessageEvent event) {
        if (!event.getResult().isAllowed()) {
            return;
        }
        final var identifier = event.getIdentifier();
        if (!(identifier.equals(AUTHME_CHANNEL) || identifier.equals(AUTHME_LEGACY_CHANNEL))) {
            return;
        }
        if (!(event.getSource() instanceof ServerConnection serverConnection)) {
            event.setResult(PluginMessageEvent.ForwardResult.handled());
            logger.debug("Blocked authme:main message from non-server source (client-side spoofing attempt?)");
            return;
        }

        event.setResult(PluginMessageEvent.ForwardResult.handled());

        ParsedMessage parsedMessage = parseMessage(event.getData());
        if (parsedMessage.typeId() == null || parsedMessage.playerName() == null) {
            return;
        }

        String serverName = serverConnection.getServer().getServerInfo().getName();

        if (LOGIN_MESSAGE.equals(parsedMessage.typeId())) {
            if (configuration.isAuthServer(serverConnection.getServer())) {
                logger.info("Player {} authenticated on auth server '{}'", parsedMessage.playerName(), serverName);
                authenticationStore.markAuthenticated(parsedMessage.playerName());
                sendAutoLoginIfAlreadySwitched(parsedMessage.playerName(), serverConnection.getServer());
                redirectToLoginServer(parsedMessage.playerName());
            } else if (pendingAutoLogins.containsKey(parsedMessage.playerName())) {
                // Implicit ACK: login from non-auth server confirms perform.login was processed
                logger.info("Auto-login confirmed for {} via login from server '{}'",
                    parsedMessage.playerName(), serverName);
                cancelPendingLogin(parsedMessage.playerName());
            } else {
                logger.debug("Ignoring login from non-auth server '{}' for {} (no pending auto-login)",
                    serverName, parsedMessage.playerName());
            }
        } else if (LOGOUT_MESSAGE.equals(parsedMessage.typeId())) {
            logger.info("Player {} logged out (notified by server '{}')", parsedMessage.playerName(), serverName);
            authenticationStore.markLoggedOut(parsedMessage.playerName());
            redirectLoggedOutPlayer(parsedMessage.playerName());
        } else if (PERFORM_LOGIN_ACK_MESSAGE.equals(parsedMessage.typeId())) {
            logger.info("Auto-login ACK received for {} from server '{}'",
                parsedMessage.playerName(), serverName);
            cancelPendingLogin(parsedMessage.playerName());
        } else if (PREMIUM_SET_MESSAGE.equals(parsedMessage.typeId())) {
            premiumUsernames.add(parsedMessage.playerName());
            pendingPremiumUsernames.remove(parsedMessage.playerName());
            logger.debug("Premium enabled for '{}' (proxy cache updated)", parsedMessage.playerName());
        } else if (PREMIUM_UNSET_MESSAGE.equals(parsedMessage.typeId())) {
            premiumUsernames.remove(parsedMessage.playerName());
            pendingPremiumUsernames.remove(parsedMessage.playerName());
            logger.debug("Premium disabled for '{}' (proxy cache updated)", parsedMessage.playerName());
        } else if (PREMIUM_PENDING_SET_MESSAGE.equals(parsedMessage.typeId())) {
            pendingPremiumUsernames.add(parsedMessage.playerName());
            logger.debug("Pending premium verification started for '{}'", parsedMessage.playerName());
        } else if (PREMIUM_LIST_MESSAGE.equals(parsedMessage.typeId())) {
            Set<String> newPremiumSet = ConcurrentHashMap.newKeySet();
            if (!parsedMessage.playerName().isEmpty()) {
                for (String name : parsedMessage.playerName().split(",")) {
                    if (!name.isEmpty()) {
                        newPremiumSet.add(name.trim());
                    }
                }
            }
            premiumUsernames = newPremiumSet;
            logger.info("Premium list received from backend: {} premium player(s)", premiumUsernames.size());
        }
    }

    void onServerConnected(ServerConnectedEvent event) {
        String playerName = event.getPlayer().getUsername();
        String newServer = event.getServer().getServerInfo().getName();
        String prevServer = event.getPreviousServer()
            .map(s -> s.getServerInfo().getName()).orElse("(none)");
        logger.debug("ServerConnected: {} moved from '{}' to '{}'", playerName, prevServer, newServer);

        sendProxyStartedHandshakeIfPending(event.getServer());

        if (!configuration.autoLoginEnabled()) {
            logger.debug("autoLogin is disabled, skipping auto-login for {}", playerName);
            return;
        }

        boolean connectingToAuthServer = configuration.isAuthServer(event.getServer());
        boolean leavingAuthServer = event.getPreviousServer()
            .map(configuration::isAuthServer).orElse(false);
        logger.debug("autoLogin check for {}: connectingToAuth={}, leavingAuth={}", playerName,
            connectingToAuthServer, leavingAuthServer);

        if (!connectingToAuthServer && !leavingAuthServer) {
            logger.debug("Skipping auto-login for {} — server transition not involving an auth server", playerName);
            return;
        }

        String normalizedName = normalizeName(playerName);

        // Pending players have passed Mojang auth at the proxy, but we must NOT send PERFORM_LOGIN
        // for them: the backend needs to run canBypassWithPremium() to finalize (persist) the premium
        // UUID. Only confirmed premium players (premiumUsernames) trigger the auto-login bypass.
        boolean isPremiumJoin = connectingToAuthServer
            && proxyVerifiedPremium.contains(normalizedName)
            && !pendingPremiumUsernames.contains(normalizedName);
        if (!authenticationStore.isAuthenticated(normalizedName) && !isPremiumJoin) {
            logger.debug("Skipping auto-login for {} — not authenticated or proxy-verified premium", normalizedName);
            return;
        }
        if (isPremiumJoin) {
            logger.debug("Proxy-verified premium player {} joining auth server — sending perform.login immediately",
                normalizedName);
        }

        Optional<ServerConnection> currentServer = event.getPlayer().getCurrentServer();
        if (currentServer.isEmpty()) {
            // Velocity hasn't registered the new connection yet; let the retry mechanism handle it
            logger.debug("Player {} has no active server connection in ServerConnectedEvent; scheduling auto-login retry", normalizedName);
            initiatePendingLogin(normalizedName);
            return;
        }

        String serverName = currentServer.get().getServer().getServerInfo().getName();
        boolean sent = currentServer.get().sendPluginMessage(AUTHME_CHANNEL, createPerformLoginMessage(normalizedName));
        if (sent) {
            logger.info("Sending auto-login request to server '{}' for player {}", serverName, normalizedName);
            initiatePendingLogin(normalizedName);
        } else {
            logger.warn("Failed to send auto-login request to server '{}' for player {}; scheduling retry", serverName, normalizedName);
            initiatePendingLogin(normalizedName);
        }
    }

    void onServerPreConnect(ServerPreConnectEvent event) {
        if (!configuration.serverSwitchRequiresAuth()) {
            return;
        }

        Player player = event.getPlayer();
        if (authenticationStore.isAuthenticated(player)) {
            return;
        }
        if (configuration.isAuthServer(event.getOriginalServer())) {
            return;
        }

        String targetName = event.getResult().getServer()
            .map(s -> s.getServerInfo().getName()).orElse("unknown");
        logger.debug("Blocking unauthenticated player {} from switching to server '{}'",
            player.getUsername(), targetName);

        event.setResult(ServerPreConnectEvent.ServerResult.denied());
        Component message = Component.text(configuration.serverSwitchKickMessage(), NamedTextColor.RED);
        if (player.getCurrentServer().isPresent()) {
            player.sendMessage(message);
        } else {
            player.disconnect(message);
        }
    }

    void onCommandExecute(CommandExecuteEvent event) {
        if (!configuration.commandsRequireAuth()) {
            return;
        }
        if (!(event.getCommandSource() instanceof Player player)) {
            return;
        }
        Optional<ServerConnection> serverOpt = player.getCurrentServer();
        if (serverOpt.isEmpty() || !configuration.isAuthServer(serverOpt.get().getServer())) {
            return;
        }
        if (authenticationStore.isAuthenticated(player)) {
            return;
        }
        if (configuration.isWhitelistedCommand(event.getCommand())) {
            return;
        }
        logger.debug("Blocking command '{}' from unauthenticated player {}", event.getCommand(), player.getUsername());
        event.setResult(CommandExecuteEvent.CommandResult.denied());
    }

    void onPlayerChat(PlayerChatEvent event) {
        if (!configuration.chatRequiresAuth()) {
            return;
        }
        Player player = event.getPlayer();
        Optional<ServerConnection> serverOpt = player.getCurrentServer();
        if (serverOpt.isEmpty() || !configuration.isAuthServer(serverOpt.get().getServer())) {
            return;
        }
        if (authenticationStore.isAuthenticated(player)) {
            return;
        }
        logger.debug("Blocking chat from unauthenticated player {}", player.getUsername());
        event.setResult(PlayerChatEvent.ChatResult.denied());
    }

    void onPreLogin(com.velocitypowered.api.event.connection.PreLoginEvent event) {
        String normalizedName = normalizeName(event.getUsername());
        if (premiumUsernames.contains(normalizedName) || pendingPremiumUsernames.contains(normalizedName)) {
            event.setResult(com.velocitypowered.api.event.connection.PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
            logger.debug("Forcing online-mode for premium player '{}'", normalizedName);
        }
    }

    void onDisconnect(DisconnectEvent event) {
        String normalizedName = normalizeName(event.getPlayer().getUsername());
        if (pendingAutoLogins.containsKey(normalizedName)) {
            logger.debug("Cancelling pending auto-login for {} (player disconnected)", normalizedName);
        }
        cancelPendingLogin(normalizedName);
        if (authenticationStore.isAuthenticated(normalizedName)) {
            logger.debug("Clearing auth state for {} (player disconnected)", normalizedName);
        }
        authenticationStore.clear(event.getPlayer());
        proxyVerifiedPremium.remove(normalizedName);
        pendingPremiumUsernames.remove(normalizedName);
    }

    void shutdown() {
        logger.info("Shutting down retry scheduler");
        retryScheduler.shutdownNow();
    }

    private void sendAutoLoginIfAlreadySwitched(String normalizedName, RegisteredServer authServer) {
        if (!configuration.autoLoginEnabled()) {
            return;
        }
        Optional<Player> playerOpt = proxyServer.getPlayer(normalizedName);
        if (playerOpt.isEmpty()) {
            return;
        }
        Optional<ServerConnection> currentConn = playerOpt.get().getCurrentServer();
        if (currentConn.isEmpty()) {
            return;
        }
        RegisteredServer currentServer = currentConn.get().getServer();
        if (currentServer.equals(authServer)) {
            // Still on auth server — normal flow, ServerConnectedEvent will handle it on switch
            return;
        }
        // Race condition: player already switched before login message arrived at the proxy
        String currentServerName = currentServer.getServerInfo().getName();
        logger.info("Player {} already on server '{}' when login message arrived — sending auto-login immediately",
            normalizedName, currentServerName);
        boolean sent = currentConn.get().sendPluginMessage(AUTHME_CHANNEL, createPerformLoginMessage(normalizedName));
        if (sent) {
            initiatePendingLogin(normalizedName);
        } else {
            logger.warn("Failed to send auto-login to '{}' for {} (race condition path)", currentServerName, normalizedName);
        }
    }

    private void sendProxyStartedHandshakeIfPending(RegisteredServer server) {
        if (!configuration.isAuthServer(server)) {
            return;
        }
        String serverName = server.getServerInfo().getName();
        if (!notifiedAuthServers.add(serverName)) {
            return;
        }
        if (server.sendPluginMessage(AUTHME_CHANNEL, createProxyStartedMessage())) {
            logger.info("Sent deferred proxy.started handshake to auth server '{}'", serverName);
        } else {
            notifiedAuthServers.remove(serverName);
            logger.info("Failed to send deferred proxy.started handshake to '{}'; scheduling retry", serverName);
            retryScheduler.schedule(() -> sendProxyStartedHandshakeIfPending(server), 1, TimeUnit.SECONDS);
        }
    }

    private void initiatePendingLogin(String normalizedName) {
        pendingAutoLogins.put(normalizedName, new AtomicInteger(0));
        scheduleRetry(normalizedName);
    }

    private void cancelPendingLogin(String normalizedName) {
        pendingAutoLogins.remove(normalizedName);
    }

    private void scheduleRetry(String normalizedName) {
        retryScheduler.schedule(() -> {
            AtomicInteger attempts = pendingAutoLogins.get(normalizedName);
            if (attempts == null) {
                return;
            }
            int current = attempts.getAndIncrement();
            if (current >= MAX_RETRIES) {
                pendingAutoLogins.remove(normalizedName);
                logger.warn("No auto-login ACK received for {} after {} retries; giving up", normalizedName, MAX_RETRIES);
                return;
            }
            Optional<Player> playerOpt = proxyServer.getPlayer(normalizedName);
            if (playerOpt.isEmpty()) {
                pendingAutoLogins.remove(normalizedName);
                logger.debug("Auto-login retry cancelled for {} (player no longer online)", normalizedName);
                return;
            }
            Optional<ServerConnection> serverOpt = playerOpt.get().getCurrentServer();
            if (serverOpt.isEmpty()) {
                pendingAutoLogins.remove(normalizedName);
                logger.debug("Auto-login retry cancelled for {} (player has no active server)", normalizedName);
                return;
            }
            String serverName = serverOpt.get().getServer().getServerInfo().getName();
            logger.debug("Retrying auto-login for {} on server '{}' (attempt {}/{})",
                normalizedName, serverName, current + 1, MAX_RETRIES);
            serverOpt.get().sendPluginMessage(AUTHME_CHANNEL, createPerformLoginMessage(normalizedName));
            scheduleRetry(normalizedName);
        }, 1, TimeUnit.SECONDS);
    }

    private ParsedMessage parseMessage(byte[] data) {
        ByteArrayDataInput input = ByteStreams.newDataInput(data);
        try {
            String typeId = input.readUTF();
            if (!LOGIN_MESSAGE.equals(typeId) && !LOGOUT_MESSAGE.equals(typeId)
                    && !PERFORM_LOGIN_ACK_MESSAGE.equals(typeId)
                    && !PREMIUM_SET_MESSAGE.equals(typeId)
                    && !PREMIUM_UNSET_MESSAGE.equals(typeId)
                    && !PREMIUM_LIST_MESSAGE.equals(typeId)
                    && !PREMIUM_PENDING_SET_MESSAGE.equals(typeId)) {
                logger.debug("Ignoring unknown authme:main message type '{}'", typeId);
                return ParsedMessage.ignored();
            }
            // premium.list carries a CSV in the second field, not a player name; read as-is
            String argument = input.readUTF();
            return new ParsedMessage(typeId,
                PREMIUM_LIST_MESSAGE.equals(typeId) ? argument : normalizeName(argument));
        } catch (IllegalStateException e) {
            logger.warn("Received malformed AuthMe plugin message on the authme:main channel");
            return ParsedMessage.ignored();
        }
    }

    private byte[] createPerformLoginMessage(String normalizedName) {
        long timestamp = System.currentTimeMillis();
        String hmac = ProxyMessageSecurity.computeHmac(configuration.sharedSecret(), normalizedName, timestamp);
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(PERFORM_LOGIN_MESSAGE);
        output.writeUTF(normalizedName);
        output.writeLong(timestamp);
        output.writeUTF(hmac);
        return output.toByteArray();
    }

    private void redirectToLoginServer(String normalizedPlayerName) {
        if (configuration.loginServer().isEmpty()) {
            return;
        }
        Optional<Player> playerOpt = proxyServer.getPlayer(normalizedPlayerName);
        if (playerOpt.isEmpty()) {
            logger.debug("Cannot redirect {} to loginServer: player no longer on proxy", normalizedPlayerName);
            return;
        }
        Optional<RegisteredServer> targetServer = proxyServer.getServer(configuration.loginServer());
        if (targetServer.isEmpty()) {
            logger.warn("loginServer '{}' is not registered on the proxy; cannot redirect {}",
                configuration.loginServer(), normalizedPlayerName);
            return;
        }
        logger.info("Redirecting {} to login server '{}' after authentication",
            normalizedPlayerName, configuration.loginServer());
        playerOpt.get().createConnectionRequest(targetServer.get()).fireAndForget();
    }

    private void redirectLoggedOutPlayer(String normalizedPlayerName) {
        if (!configuration.sendOnLogoutEnabled()) {
            return;
        }
        if (configuration.sendOnLogoutTarget().isEmpty()) {
            logger.warn("Received logout for {} but sendOnLogout has no configured target server", normalizedPlayerName);
            return;
        }

        Optional<Player> player = proxyServer.getPlayer(normalizedPlayerName);
        if (player.isEmpty()) {
            logger.debug("Received logout for {} but they are no longer on the proxy", normalizedPlayerName);
            return;
        }

        Optional<RegisteredServer> targetServer = proxyServer.getServer(configuration.sendOnLogoutTarget());
        if (targetServer.isEmpty()) {
            logger.warn("Received logout for {} but target server '{}' is not registered on the proxy",
                normalizedPlayerName, configuration.sendOnLogoutTarget());
            return;
        }

        logger.info("Redirecting {} to server '{}' after logout", normalizedPlayerName, configuration.sendOnLogoutTarget());
        ConnectionRequestBuilder connectionRequest = player.get().createConnectionRequest(targetServer.get());
        connectionRequest.fireAndForget();
    }

    private static String normalizeName(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }

    private record ParsedMessage(String typeId, String playerName) {

        private static ParsedMessage ignored() {
            return new ParsedMessage(null, null);
        }
    }
}
