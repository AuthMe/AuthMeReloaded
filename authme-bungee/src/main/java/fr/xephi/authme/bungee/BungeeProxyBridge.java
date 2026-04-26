package fr.xephi.authme.bungee;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public final class BungeeProxyBridge implements Listener {

    static final String AUTHME_CHANNEL = "authme:main";

    private static final String LOGIN_MESSAGE = "login";
    private static final String LOGOUT_MESSAGE = "logout";
    private static final String PERFORM_LOGIN_MESSAGE = "perform.login";
    private static final String PERFORM_LOGIN_ACK_MESSAGE = "perform.login.ack";
    private static final String PROXY_STARTED_MESSAGE = "proxy.started";
    private static final String PROXY_IDENTITY = "bungee";
    private static final int MAX_RETRIES = 3;

    private final ProxyServer proxyServer;
    private final Logger logger;
    private BungeeProxyConfiguration configuration;
    private final BungeeAuthenticationStore authenticationStore;
    private final Map<String, AtomicInteger> pendingAutoLogins = new ConcurrentHashMap<>();
    private final Set<String> notifiedAuthServers = ConcurrentHashMap.newKeySet();
    private final ScheduledExecutorService retryScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "authme-bungee-retry");
        t.setDaemon(true);
        return t;
    });

    BungeeProxyBridge(ProxyServer proxyServer, Logger logger, BungeeProxyConfiguration configuration,
                      BungeeAuthenticationStore authenticationStore) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.configuration = configuration;
        this.authenticationStore = authenticationStore;
    }

    void reload(BungeeProxyConfiguration configuration) {
        this.configuration = configuration;
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

        if (configuration.sendOnLogoutEnabled() && configuration.sendOnLogoutTarget().isEmpty()) {
            logger.warning("sendOnLogout is enabled but unloggedUserServer is empty; logout redirects will be skipped");
        }
    }

    void registerChannels() {
        proxyServer.registerChannel(AUTHME_CHANNEL);
        logger.info("Registered AuthMe BungeeCord bridge channel");
        broadcastProxyStartedHandshake();
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

        if (currentServer == null || !authenticationStore.isAuthenticated(player)) {
            return;
        }

        boolean connectingToAuthServer = configuration.isAuthServer(currentServer.getInfo());
        boolean leavingAuthServer = event.getFrom() != null && configuration.isAuthServer(event.getFrom());
        if (!connectingToAuthServer && !leavingAuthServer) {
            return;
        }

        String normalizedName = normalizeName(player.getName());
        String serverName = currentServer.getInfo().getName();
        logger.info("Sending auto-login request to server '" + serverName + "' for player " + normalizedName);
        currentServer.getInfo().sendData(AUTHME_CHANNEL, createPerformLoginMessage(normalizedName), false);
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
    }

    void shutdown() {
        proxyServer.unregisterChannel(AUTHME_CHANNEL);
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
        currentConn.getInfo().sendData(AUTHME_CHANNEL, createPerformLoginMessage(normalizedName), false);
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
            server.getInfo().sendData(AUTHME_CHANNEL, createPerformLoginMessage(normalizedName), false);
            scheduleRetry(normalizedName);
        }, 1, TimeUnit.SECONDS);
    }

    private ParsedPluginMessage parsePluginMessage(byte[] data) {
        ByteArrayDataInput input = ByteStreams.newDataInput(data);

        try {
            String typeId = input.readUTF();
            if (!LOGIN_MESSAGE.equals(typeId) && !LOGOUT_MESSAGE.equals(typeId)
                    && !PERFORM_LOGIN_ACK_MESSAGE.equals(typeId)) {
                return ParsedPluginMessage.ignored();
            }
            return new ParsedPluginMessage(typeId, normalizeName(input.readUTF()));
        } catch (IllegalStateException e) {
            logger.warning("Received malformed AuthMe plugin message on the authme:main channel");
            return ParsedPluginMessage.ignored();
        }
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

    private static String normalizeName(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }

    private record ParsedPluginMessage(String typeId, String playerName) {

        private static ParsedPluginMessage ignored() {
            return new ParsedPluginMessage(null, null);
        }
    }
}
