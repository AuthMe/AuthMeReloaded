package fr.xephi.authme.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(
    id = AbstractAuthMeVelocityPlugin.PLUGIN_ID,
    name = AbstractAuthMeVelocityPlugin.PLUGIN_NAME,
    version = AbstractAuthMeVelocityPlugin.PLUGIN_VERSION,
    description = "Velocity proxy bridge for AuthMe inter-server authentication",
    authors = {"AuthMe-Team"},
    url = "https://github.com/AuthMe/AuthMeReloaded")
public final class AuthMeVelocityPlugin extends AbstractAuthMeVelocityPlugin {

    private final ProxyServer server;
    private VelocityConfigManager configManager;
    private final Logger logger;
    private final VelocityProxyBridge proxyBridge;

    @Inject
    public AuthMeVelocityPlugin(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        super(server, logger, dataDirectory);
        this.server = server;
        this.logger = logger;
        this.proxyBridge = createProxyBridge(server, logger, dataDirectory);
    }

    protected VelocityProxyBridge createProxyBridge(ProxyServer server, Logger logger, Path dataDirectory) {
        this.configManager = new VelocityConfigManager(dataDirectory);
        VelocityAuthenticationStore authenticationStore = new VelocityAuthenticationStore();
        return new VelocityProxyBridge(server, logger, configManager.getConfiguration(), authenticationStore);
    }

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        proxyBridge.registerChannels();
        proxyBridge.logConfigurationDetails();
        CommandMeta meta = server.getCommandManager().metaBuilder("avreloadproxy")
            .plugin(this)
            .build();
        server.getCommandManager().register(meta, new VelocityReloadCommand(configManager, proxyBridge));
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        proxyBridge.onPluginMessage(event);
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        proxyBridge.onServerConnected(event);
    }

    @Subscribe
    public void onServerPreConnect(ServerPreConnectEvent event) {
        proxyBridge.onServerPreConnect(event);
    }

    @Subscribe
    public void onCommandExecute(CommandExecuteEvent event) {
        proxyBridge.onCommandExecute(event);
    }

    @Subscribe
    public void onPlayerChat(PlayerChatEvent event) {
        proxyBridge.onPlayerChat(event);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        proxyBridge.onDisconnect(event);
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        proxyBridge.shutdown();
    }
}
