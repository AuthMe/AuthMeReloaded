package fr.xephi.authme.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.bungee.premium.BungeePremiumOnlineModeHandler;
import fr.xephi.authme.bungee.premium.BungeePremiumVerificationManager;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PlayerHandshakeEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.UUID;
import java.util.logging.Logger;

public final class BungeeProxyBridge implements Listener {

    static final String AUTHME_CHANNEL = "authme:main";

    private static final String LOGIN_MESSAGE = "login";
    private static final String LOGOUT_MESSAGE = "logout";
    private static final String PERFORM_LOGIN_MESSAGE = "perform.login";
    private static final String PERFORM_LOGIN_ACK_MESSAGE = "perform.login.ack";
    private static final String PROXY_STARTED_MESSAGE = "proxy.started";
    private static final String PREMIUM_SET_MESSAGE = "premium.set";
    private static final String PREMIUM_UNSET_MESSAGE = "premium.unset";
    private static final String PREMIUM_LIST_MESSAGE = "premium.list";
    private static final String PREMIUM_LIST_CHUNK_MESSAGE = "premium.list.chunk";
    private static final String PREMIUM_PENDING_SET_MESSAGE = "premium.pending.set";
    private static final String PROXY_IDENTITY = "bungee";
    private static final int MAX_RETRIES = 3;

    private final ProxyServer proxyServer;
    private final Logger logger;
    private BungeeProxyConfiguration configuration;
    private final BungeeAuthenticationStore authenticationStore;
    private final Map<String, AtomicInteger> pendingAutoLogins = new ConcurrentHashMap<>();
    private final Set<String> notifiedAuthServers = ConcurrentHashMap.newKeySet();
    private volatile Set<String> premiumUsernames = ConcurrentHashMap.newKeySet();
    private List<String> premiumListBuffer = new ArrayList<>();
    // Players with a pending premium verification (ran /premium but not yet confirmed via reconnect)
    private volatile Set<String> pendingPremiumUsernames = ConcurrentHashMap.newKeySet();
    private final BungeePremiumOnlineModeHandler premiumOnlineModeHandler;
    private final BungeePremiumVerificationManager premiumVerificationManager;
    private final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "authme-bungee-retry");
        t.setDaemon(true);
        return t;
    });
    private final Path cacheFile;

    BungeeProxyBridge(ProxyServer proxyServer, Logger logger, BungeeProxyConfiguration configuration,
                      BungeeAuthenticationStore authenticationStore, Path dataDirectory) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.configuration = configuration;
        this.authenticationStore = authenticationStore;
        this.premiumOnlineModeHandler = new BungeePremiumOnlineModeHandler(this::requiresPremiumVerification);
        this.premiumVerificationManager =
            new BungeePremiumVerificationManager(proxyServer, logger,
                this::requiresPremiumVerification, this::isPendingPremiumVerification,
                this::clearPendingPremiumVerification,
                () -> this.configuration.keepOfflineUuidCompatibility());
        this.cacheFile = dataDirectory != null ? dataDirectory.resolve("premium_names.cache") : null;
        if (cacheFile != null) {
            loadPremiumNamesCache();
        }
    }

    private void loadPremiumNamesCache() {
        Set<String> loaded = new HashSet<>();
        try (BufferedReader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String name = line.trim();
                if (!name.isEmpty()) {
                    loaded.add(name);
                }
            }
            Set<String> newSet = ConcurrentHashMap.newKeySet();
            newSet.addAll(loaded);
            premiumUsernames = newSet;
            logger.info("Loaded " + loaded.size() + " premium username(s) from cache");
        } catch (NoSuchFileException ignored) {
            // No cache yet — first startup
        } catch (IOException e) {
            logger.warning("Failed to load premium names cache: " + e.getMessage());
        }
    }

    private void savePremiumNamesAsync() {
        if (cacheFile == null) {
            return;
        }
        Set<String> snapshot = new HashSet<>(premiumUsernames);
        retryScheduler.execute(() -> {
            Path tmp = cacheFile.resolveSibling(cacheFile.getFileName() + ".tmp");
            try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                for (String name : snapshot) {
                    writer.write(name);
                    writer.newLine();
                }
                Files.move(tmp, cacheFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                logger.warning("Failed to save premium names cache: " + e.getMessage());
            }
        });
    }

    void reload(BungeeProxyConfiguration configuration) {
        this.configuration = configuration;
        premiumVerificationManager.refreshRegistration();
        logger.info("Configuration reloaded");
    }

    void logConfigurationDetails() {
        if (configuration.allServersAreAuthServers()) {
            logger.info("All registered backend servers are treated as auth servers");
        } else if (configuration.authServers().isEmpty()) {
            logger.warning("No auth servers are configured; autoLogin will only work after authServers is populated"
                + " or allServersAreAuthServers is enabled");
        } else {
            logger.info("Current auth servers:");
            configuration.authServers().forEach(serverName -> logger.info("> " + serverName));
        }

        if (!configuration.autoLoginEnabled()) {
            logger.info("autoLogin is disabled");
        }

        logger.info("premium.keepOfflineUuidCompatibility is "
            + (configuration.keepOfflineUuidCompatibility() ? "enabled" : "disabled"));

        if (configuration.sendOnLogoutEnabled() && configuration.sendOnLogoutTarget().isEmpty()) {
            logger.warning("sendOnLogout is enabled but unloggedUserServer is empty; logout redirects will be skipped");
        }
    }

    void registerChannels() {
        proxyServer.registerChannel(AUTHME_CHANNEL);
        logger.info("Registered AuthMe BungeeCord bridge channel");
        broadcastProxyStartedHandshake();
        premiumVerificationManager.register();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerHandshake(PlayerHandshakeEvent event) {
        if (!configuration.keepOfflineUuidCompatibility()) {
            premiumOnlineModeHandler.enableOnlineModeIfRequired(event.getConnection());
        }
    }

    private boolean requiresPremiumVerification(String normalizedName) {
        return premiumUsernames.contains(normalizedName) || pendingPremiumUsernames.contains(normalizedName);
    }

    private boolean isPendingPremiumVerification(String normalizedName) {
        return pendingPremiumUsernames.contains(normalizedName);
    }

    private void clearPendingPremiumVerification(String normalizedName) {
        if (pendingPremiumUsernames.remove(normalizedName)) {
            premiumVerificationManager.clearVerifiedPremium(normalizedName);
            logger.warning("Cleared pending premium verification for '" + normalizedName
                + "' after a failed proxy-side premium handshake");
        }
    }

    void broadcastProxyStartedHandshake() {
        byte[] payload = createProxyStartedMessage();
        int notified = 0;
        int deferred = 0;
        for (ServerInfo server : proxyServer.getServers().values()) {
            if (!configuration.isAuthServer(server)) {
                continue;
            }
            String serverName = server.getName();
            if (!server.getPlayers().isEmpty()) {
                server.sendData(AUTHME_CHANNEL, payload, false);
                notifiedAuthServers.add(serverName);
                notified++;
                logger.info("Sent proxy.started handshake to auth server '" + serverName + "'");
            } else {
                deferred++;
                logger.info("Deferred proxy.started handshake for '" + serverName + "' (no connected players yet);"
                    + " will retry on first player connection");
            }
        }
        if (notified > 0 || deferred > 0) {
            logger.info("Proxy startup handshake: " + notified + " auth server(s) notified, "
                + deferred + " deferred until first connection");
        }
    }

    private byte[] createProxyStartedMessage() {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(PROXY_STARTED_MESSAGE);
        output.writeUTF(PROXY_IDENTITY);
        return output.toByteArray();
    }

    @EventHandler
    public void onPluginMessage(PluginMessageEvent event) {
        if (event.isCancelled() || !AUTHME_CHANNEL.equals(event.getTag())) {
            return;
        }
        if (!(event.getSender() instanceof Server server)) {
            event.setCancelled(true);
            return;
        }

        ParsedPluginMessage parsedMessage = parsePluginMessage(event.getData());
        if (parsedMessage.typeId() == null || parsedMessage.playerName() == null) {
            return;
        }

        if (LOGIN_MESSAGE.equals(parsedMessage.typeId())) {
            if (configuration.isAuthServer(server.getInfo())) {
                logger.info("Player " + parsedMessage.playerName() + " authenticated on auth server '"
                    + server.getInfo().getName() + "'");
                authenticationStore.markAuthenticated(parsedMessage.playerName());
                sendAutoLoginIfAlreadySwitched(parsedMessage.playerName(), server.getInfo());
                redirectToLoginServer(parsedMessage.playerName());
            } else if (pendingAutoLogins.containsKey(parsedMessage.playerName())) {
                // Implicit ACK: login from non-auth server confirms perform.login was processed
                logger.info("Auto-login confirmed for " + parsedMessage.playerName()
                    + " via login from server '" + server.getInfo().getName() + "'");
                cancelPendingLogin(parsedMessage.playerName());
            }
        } else if (LOGOUT_MESSAGE.equals(parsedMessage.typeId())) {
            authenticationStore.markLoggedOut(parsedMessage.playerName());
            redirectLoggedOutPlayer(parsedMessage.playerName());
        } else if (PERFORM_LOGIN_ACK_MESSAGE.equals(parsedMessage.typeId())) {
            logger.info("Auto-login ACK received for " + parsedMessage.playerName()
                + " from server '" + server.getInfo().getName() + "'");
            cancelPendingLogin(parsedMessage.playerName());
        } else if (PREMIUM_SET_MESSAGE.equals(parsedMessage.typeId())) {
            premiumUsernames.add(parsedMessage.playerName());
            pendingPremiumUsernames.remove(parsedMessage.playerName());
            logger.fine(() -> "Premium enabled for '" + parsedMessage.playerName() + "' (proxy cache updated)");
            savePremiumNamesAsync();
        } else if (PREMIUM_UNSET_MESSAGE.equals(parsedMessage.typeId())) {
            premiumUsernames.remove(parsedMessage.playerName());
            pendingPremiumUsernames.remove(parsedMessage.playerName());
            premiumVerificationManager.clearVerifiedPremium(parsedMessage.playerName());
            logger.fine(() -> "Premium disabled for '" + parsedMessage.playerName() + "' (proxy cache updated)");
            savePremiumNamesAsync();
        } else if (PREMIUM_PENDING_SET_MESSAGE.equals(parsedMessage.typeId())) {
            pendingPremiumUsernames.add(parsedMessage.playerName());
            premiumVerificationManager.clearVerifiedPremium(parsedMessage.playerName());
            logger.fine(() -> "Pending premium verification started for '" + parsedMessage.playerName() + "'");
        } else if (PREMIUM_LIST_MESSAGE.equals(parsedMessage.typeId())) {
            Set<String> newPremiumSet = ConcurrentHashMap.newKeySet();
            if (!parsedMessage.playerName().isEmpty()) {
                for (String name : parsedMessage.playerName().split(",")) {
                    if (!name.isEmpty()) {
                        newPremiumSet.add(normalizeName(name.trim()));
                    }
                }
            }
            premiumUsernames = newPremiumSet;
            logger.info("Premium list received from backend: " + premiumUsernames.size() + " premium player(s)");
        } else if (PREMIUM_LIST_CHUNK_MESSAGE.equals(parsedMessage.typeId())) {
            String[] parts = parsedMessage.playerName().split(":", 3);
            if (parts.length < 3) {
                logger.warning("Malformed premium.list.chunk payload: " + parsedMessage.playerName());
                return;
            }
            if ("0".equals(parts[0])) {
                premiumListBuffer = new ArrayList<>();
            }
            String csv = parts[2];
            if (!csv.isEmpty()) {
                for (String name : csv.split(",")) {
                    if (!name.isEmpty()) {
                        premiumListBuffer.add(normalizeName(name.trim()));
                    }
                }
            }
            if ("1".equals(parts[1])) {
                Set<String> newPremiumSet = ConcurrentHashMap.newKeySet();
                newPremiumSet.addAll(premiumListBuffer);
                premiumUsernames = newPremiumSet;
                premiumListBuffer = new ArrayList<>();
                logger.info("Premium list received from backend: " + premiumUsernames.size() + " premium player(s)");
                savePremiumNamesAsync();
            }
        }
    }

    @EventHandler
    public void onServerSwitch(ServerSwitchEvent event) {
        ProxiedPlayer player = event.getPlayer();
        Server currentServer = player.getServer();
        if (currentServer != null) {
            sendProxyStartedHandshakeIfPending(currentServer.getInfo());
        }

        if (!configuration.autoLoginEnabled()) {
            return;
        }

        if (currentServer == null) {
            return;
        }

        boolean connectingToAuthServer = configuration.isAuthServer(currentServer.getInfo());
        boolean leavingAuthServer = event.getFrom() != null && configuration.isAuthServer(event.getFrom());
        if (!connectingToAuthServer && !leavingAuthServer) {
            return;
        }

        String normalizedName = normalizeName(player.getName());
        UUID verifiedPremiumUuid = premiumVerificationManager.getVerifiedPremiumUuid(normalizedName);
        boolean isPremiumJoin = connectingToAuthServer && verifiedPremiumUuid != null;
        if (!authenticationStore.isAuthenticated(player) && !isPremiumJoin) {
            return;
        }
        if (isPremiumJoin) {
            logger.fine("PacketEvents-verified premium player " + normalizedName
                + " joining auth server — sending perform.login immediately");
        }

        String serverName = currentServer.getInfo().getName();
        logger.info("Sending auto-login request to server '" + serverName + "' for player " + normalizedName);
        currentServer.getInfo().sendData(
            AUTHME_CHANNEL, createPerformLoginMessage(normalizedName, verifiedPremiumUuid), false);
        initiatePendingLogin(normalizedName);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(ChatEvent event) {
        if (event.isCancelled() || !event.isCommand() || !configuration.commandsRequireAuth()) {
            return;
        }
        if (!(event.getSender() instanceof ProxiedPlayer player)) {
            return;
        }
        if (authenticationStore.isAuthenticated(player) || player.getServer() == null
            || !configuration.isAuthServer(player.getServer().getInfo())) {
            return;
        }
        if (configuration.isWhitelistedCommand(event.getMessage())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(ChatEvent event) {
        if (event.isCancelled() || event.isCommand() || !configuration.chatRequiresAuth()) {
            return;
        }
        if (!(event.getSender() instanceof ProxiedPlayer player)) {
            return;
        }
        if (authenticationStore.isAuthenticated(player) || player.getServer() == null
            || !configuration.isAuthServer(player.getServer().getInfo())) {
            return;
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerConnectingToServer(ServerConnectEvent event) {
        if (event.isCancelled() || !configuration.serverSwitchRequiresAuth()) {
            return;
        }

        ProxiedPlayer player = event.getPlayer();
        if (authenticationStore.isAuthenticated(player) || configuration.isAuthServer(event.getTarget())) {
            return;
        }

        event.setCancelled(true);
        TextComponent reasonMessage = new TextComponent(configuration.serverSwitchKickMessage());
        reasonMessage.setColor(ChatColor.RED);
        if (player.getServer() == null) {
            player.disconnect(reasonMessage);
        } else {
            player.sendMessage(reasonMessage);
        }
    }

    @EventHandler
    public void onPlayerDisconnect(PlayerDisconnectEvent event) {
        String normalizedName = normalizeName(event.getPlayer().getName());
        if (pendingAutoLogins.containsKey(normalizedName)) {
            logger.fine("Cancelling pending auto-login for " + normalizedName + " (player disconnected)");
        }
        cancelPendingLogin(normalizedName);
        authenticationStore.clear(event.getPlayer());
        premiumVerificationManager.clearVerifiedPremium(normalizedName);
    }

    void shutdown() {
        proxyServer.unregisterChannel(AUTHME_CHANNEL);
        premiumVerificationManager.shutdown();
        retryScheduler.shutdownNow();
    }

    private void sendAutoLoginIfAlreadySwitched(String normalizedName, ServerInfo authServer) {
        if (!configuration.autoLoginEnabled()) {
            return;
        }
        ProxiedPlayer player = proxyServer.getPlayer(normalizedName);
        if (player == null) {
            return;
        }
        Server currentConn = player.getServer();
        if (currentConn == null) {
            return;
        }
        if (currentConn.getInfo().equals(authServer)) {
            // Still on auth server — normal flow, ServerSwitchEvent will handle it on switch
            return;
        }
        String currentServerName = currentConn.getInfo().getName();
        logger.info("Player " + normalizedName + " already on server '" + currentServerName
            + "' when login message arrived — sending auto-login immediately");
        UUID verifiedPremiumUuid = premiumVerificationManager.getVerifiedPremiumUuid(normalizedName);
        currentConn.getInfo().sendData(
            AUTHME_CHANNEL, createPerformLoginMessage(normalizedName, verifiedPremiumUuid), false);
        initiatePendingLogin(normalizedName);
    }

    private void sendProxyStartedHandshakeIfPending(ServerInfo server) {
        if (!configuration.isAuthServer(server)) {
            return;
        }
        String serverName = server.getName();
        if (!notifiedAuthServers.add(serverName)) {
            return;
        }
        if (!server.getPlayers().isEmpty()) {
            server.sendData(AUTHME_CHANNEL, createProxyStartedMessage(), false);
            logger.info("Sent deferred proxy.started handshake to auth server '" + serverName + "'");
        } else {
            notifiedAuthServers.remove(serverName);
            logger.info("Failed to send deferred proxy.started handshake to '" + serverName + "'; scheduling retry");
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
                logger.warning("No auto-login ACK received for " + normalizedName
                    + " after " + MAX_RETRIES + " retries; giving up");
                return;
            }
            ProxiedPlayer player = proxyServer.getPlayer(normalizedName);
            if (player == null) {
                pendingAutoLogins.remove(normalizedName);
                logger.fine("Auto-login retry cancelled for " + normalizedName + " (player no longer online)");
                return;
            }
            Server server = player.getServer();
            if (server == null) {
                pendingAutoLogins.remove(normalizedName);
                logger.fine("Auto-login retry cancelled for " + normalizedName + " (player has no active server)");
                return;
            }
            String serverName = server.getInfo().getName();
            logger.fine("Retrying auto-login for " + normalizedName + " on server '" + serverName
                + "' (attempt " + (current + 1) + "/" + MAX_RETRIES + ")");
            UUID verifiedPremiumUuid = premiumVerificationManager.getVerifiedPremiumUuid(normalizedName);
            server.getInfo().sendData(
                AUTHME_CHANNEL, createPerformLoginMessage(normalizedName, verifiedPremiumUuid), false);
            scheduleRetry(normalizedName);
        }, 1, TimeUnit.SECONDS);
    }

    private ParsedPluginMessage parsePluginMessage(byte[] data) {
        ByteArrayDataInput input = ByteStreams.newDataInput(data);

        try {
            String typeId = input.readUTF();
            if (!LOGIN_MESSAGE.equals(typeId) && !LOGOUT_MESSAGE.equals(typeId)
                    && !PERFORM_LOGIN_ACK_MESSAGE.equals(typeId)
                    && !PREMIUM_SET_MESSAGE.equals(typeId)
                    && !PREMIUM_UNSET_MESSAGE.equals(typeId)
                    && !PREMIUM_LIST_MESSAGE.equals(typeId)
                    && !PREMIUM_LIST_CHUNK_MESSAGE.equals(typeId)
                    && !PREMIUM_PENDING_SET_MESSAGE.equals(typeId)) {
                return ParsedPluginMessage.ignored();
            }
            String argument = input.readUTF();
            return new ParsedPluginMessage(typeId,
                (PREMIUM_LIST_MESSAGE.equals(typeId) || PREMIUM_LIST_CHUNK_MESSAGE.equals(typeId))
                    ? argument : normalizeName(argument));
        } catch (IllegalStateException e) {
            logger.warning("Received malformed AuthMe plugin message on the authme:main channel");
            return ParsedPluginMessage.ignored();
        }
    }

    private void redirectToLoginServer(String normalizedPlayerName) {
        if (configuration.loginServer().isEmpty()) {
            return;
        }
        ProxiedPlayer player = proxyServer.getPlayer(normalizedPlayerName);
        if (player == null) {
            logger.fine("Cannot redirect " + normalizedPlayerName + " to loginServer: player no longer on proxy");
            return;
        }
        ServerInfo targetServer = proxyServer.getServerInfo(configuration.loginServer());
        if (targetServer == null) {
            logger.warning("loginServer '" + configuration.loginServer()
                + "' is not registered on the proxy; cannot redirect " + normalizedPlayerName);
            return;
        }
        logger.info("Redirecting " + normalizedPlayerName + " to login server '"
            + configuration.loginServer() + "' after authentication");
        player.connect(targetServer);
    }

    private void redirectLoggedOutPlayer(String normalizedPlayerName) {
        if (!configuration.sendOnLogoutEnabled()) {
            return;
        }
        if (configuration.sendOnLogoutTarget().isEmpty()) {
            logger.warning("Received logout for " + normalizedPlayerName
                + " but sendOnLogout has no configured target server");
            return;
        }

        ProxiedPlayer player = proxyServer.getPlayer(normalizedPlayerName);
        if (player == null) {
            return;
        }

        ServerInfo targetServer = proxyServer.getServerInfo(configuration.sendOnLogoutTarget());
        if (targetServer == null) {
            logger.warning("Received logout for " + normalizedPlayerName + " but target server '"
                + configuration.sendOnLogoutTarget() + "' is not registered on the proxy");
            return;
        }

        player.connect(targetServer);
    }

    private byte[] createPerformLoginMessage(String normalizedName, UUID verifiedPremiumUuid) {
        long timestamp = System.currentTimeMillis();
        String hmac = ProxyMessageSecurity.computeHmac(
            configuration.sharedSecret(), normalizedName, timestamp, verifiedPremiumUuid);
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(PERFORM_LOGIN_MESSAGE);
        output.writeUTF(normalizedName);
        output.writeLong(timestamp);
        output.writeUTF(verifiedPremiumUuid == null ? "" : verifiedPremiumUuid.toString());
        output.writeUTF(hmac);
        return output.toByteArray();
    }

    private static String normalizeName(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }

    private record ParsedPluginMessage(String typeId, String playerName) {

        private static ParsedPluginMessage ignored() {
            return new ParsedPluginMessage(null, null);
        }
    }
}
