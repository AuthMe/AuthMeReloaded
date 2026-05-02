package fr.xephi.authme.velocity;

import ch.jalu.configme.SettingsManager;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import fr.xephi.authme.velocity.config.VelocityConfigProperties;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

final class VelocityProxyConfiguration {

    private final Set<String> authServers;
    private final boolean allServersAreAuthServers;
    private final boolean serverSwitchRequiresAuth;
    private final String serverSwitchKickMessage;
    private final boolean autoLoginEnabled;
    private final boolean sendOnLogoutEnabled;
    private final String sendOnLogoutTarget;
    private final boolean commandsRequireAuth;
    private final Set<String> commandWhitelist;
    private final boolean chatRequiresAuth;
    private final String loginServer;
    private final String sharedSecret;

    VelocityProxyConfiguration(Set<String> authServers, boolean allServersAreAuthServers,
                               boolean serverSwitchRequiresAuth, String serverSwitchKickMessage,
                               boolean autoLoginEnabled, boolean sendOnLogoutEnabled,
                               String sendOnLogoutTarget, boolean commandsRequireAuth,
                               Set<String> commandWhitelist, boolean chatRequiresAuth,
                               String loginServer, String sharedSecret) {
        this.authServers = authServers;
        this.allServersAreAuthServers = allServersAreAuthServers;
        this.serverSwitchRequiresAuth = serverSwitchRequiresAuth;
        this.serverSwitchKickMessage = serverSwitchKickMessage;
        this.autoLoginEnabled = autoLoginEnabled;
        this.sendOnLogoutEnabled = sendOnLogoutEnabled;
        this.sendOnLogoutTarget = normalizeServerName(sendOnLogoutTarget);
        this.commandsRequireAuth = commandsRequireAuth;
        this.commandWhitelist = commandWhitelist;
        this.chatRequiresAuth = chatRequiresAuth;
        this.loginServer = normalizeServerName(loginServer);
        this.sharedSecret = sharedSecret;
    }

    static VelocityProxyConfiguration from(SettingsManager settingsManager) {
        return new VelocityProxyConfiguration(
            normalizeServerNames(settingsManager.getProperty(VelocityConfigProperties.AUTH_SERVERS)),
            settingsManager.getProperty(VelocityConfigProperties.ALL_SERVERS_ARE_AUTH_SERVERS),
            settingsManager.getProperty(VelocityConfigProperties.SERVER_SWITCH_REQUIRES_AUTH),
            settingsManager.getProperty(VelocityConfigProperties.SERVER_SWITCH_KICK_MESSAGE),
            settingsManager.getProperty(VelocityConfigProperties.AUTOLOGIN),
            settingsManager.getProperty(VelocityConfigProperties.ENABLE_SEND_ON_LOGOUT),
            settingsManager.getProperty(VelocityConfigProperties.SEND_ON_LOGOUT_TARGET),
            settingsManager.getProperty(VelocityConfigProperties.COMMANDS_REQUIRE_AUTH),
            normalizeCommandAliases(settingsManager.getProperty(VelocityConfigProperties.COMMANDS_WHITELIST)),
            settingsManager.getProperty(VelocityConfigProperties.CHAT_REQUIRES_AUTH),
            settingsManager.getProperty(VelocityConfigProperties.LOGIN_SERVER),
            settingsManager.getProperty(VelocityConfigProperties.PROXY_SHARED_SECRET));
    }

    Set<String> authServers() {
        return authServers;
    }

    boolean allServersAreAuthServers() {
        return allServersAreAuthServers;
    }

    boolean serverSwitchRequiresAuth() {
        return serverSwitchRequiresAuth;
    }

    String serverSwitchKickMessage() {
        return serverSwitchKickMessage;
    }

    boolean autoLoginEnabled() {
        return autoLoginEnabled;
    }

    boolean sendOnLogoutEnabled() {
        return sendOnLogoutEnabled;
    }

    String sendOnLogoutTarget() {
        return sendOnLogoutTarget;
    }

    boolean commandsRequireAuth() {
        return commandsRequireAuth;
    }

    boolean chatRequiresAuth() {
        return chatRequiresAuth;
    }

    String loginServer() {
        return loginServer;
    }

    String sharedSecret() {
        return sharedSecret;
    }

    boolean isAuthServer(RegisteredServer server) {
        return allServersAreAuthServers || authServers.contains(normalizeServerName(server.getServerInfo().getName()));
    }

    boolean isWhitelistedCommand(String command) {
        return commandWhitelist.contains(normalizeCommandAlias(command));
    }

    private static Set<String> normalizeServerNames(Collection<String> serverNames) {
        LinkedHashSet<String> normalizedServers = new LinkedHashSet<>();
        for (String serverName : serverNames) {
            String normalizedServerName = normalizeServerName(serverName);
            if (!normalizedServerName.isEmpty()) {
                normalizedServers.add(normalizedServerName);
            }
        }
        return Set.copyOf(normalizedServers);
    }

    private static String normalizeServerName(String serverName) {
        return serverName.trim().toLowerCase(Locale.ROOT);
    }

    static String normalizeCommandAlias(String command) {
        String trimmedCommand = command.trim();
        if (trimmedCommand.isEmpty()) {
            return "";
        }
        int firstWhitespace = trimmedCommand.indexOf(' ');
        String commandAlias = firstWhitespace >= 0 ? trimmedCommand.substring(0, firstWhitespace) : trimmedCommand;
        if (!commandAlias.startsWith("/")) {
            commandAlias = "/" + commandAlias;
        }
        return commandAlias.toLowerCase(Locale.ROOT);
    }

    private static Set<String> normalizeCommandAliases(List<String> aliases) {
        return aliases.stream()
            .map(VelocityProxyConfiguration::normalizeCommandAlias)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }
}
