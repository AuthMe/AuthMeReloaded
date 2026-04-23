package fr.xephi.authme.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.event.command.CommandExecuteEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.PlayerChatEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelRegistrar;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class VelocityProxyBridgeTest {

    @Mock
    private ProxyServer proxyServer;

    @Mock
    private Logger logger;

    @Mock
    private ChannelRegistrar channelRegistrar;

    @Mock
    private PluginMessageEvent pluginMessageEvent;

    @Mock
    private ServerConnection sourceConnection;

    @Mock
    private Player player;

    @Mock
    private ServerConnection currentServer;

    @Mock
    private RegisteredServer authServer;

    @Mock
    private RegisteredServer nonAuthServer;

    @Mock
    private ServerInfo authServerInfo;

    @Mock
    private ServerInfo nonAuthServerInfo;

    @Mock
    private ConnectionRequestBuilder connectionRequest;

    @Mock
    private CommandExecuteEvent commandEvent;

    @Mock
    private PlayerChatEvent chatEvent;

    @Mock
    private CommandSource consoleSource;

    @Captor
    private ArgumentCaptor<byte[]> payloadCaptor;

    @Test
    void shouldRegisterAuthMeChannel() {
        given(proxyServer.getChannelRegistrar()).willReturn(channelRegistrar);

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.registerChannels();

        verify(channelRegistrar).register(VelocityProxyBridge.AUTHME_CHANNEL, VelocityProxyBridge.AUTHME_LEGACY_CHANNEL);
    }

    @Test
    void shouldTrackAuthenticatedPlayerAndForwardPerformLoginOnServerConnect() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.forward());
        given(pluginMessageEvent.getIdentifier()).willReturn(VelocityProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSource()).willReturn(sourceConnection);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceConnection.getServer()).willReturn(authServer);
        given(player.getUsername()).willReturn("Alice");
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");
        given(currentServer.getServer()).willReturn(authServer);
        given(currentServer.sendPluginMessage(eq(VelocityProxyBridge.AUTHME_CHANNEL), any(byte[].class)))
            .willReturn(true);

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerConnected(new ServerConnectedEvent(player, authServer, null));

        verify(pluginMessageEvent).setResult(any(PluginMessageEvent.ForwardResult.class));
        verify(currentServer).sendPluginMessage(eq(VelocityProxyBridge.AUTHME_CHANNEL), payloadCaptor.capture());
        assertPerformLoginPayload(payloadCaptor.getValue(), "alice", "test-secret");
    }

    @Test
    void shouldIgnoreAlreadyHandledPluginMessage() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.handled());

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);

        verify(pluginMessageEvent, never()).getIdentifier();
        verify(pluginMessageEvent, never()).setResult(any());
    }

    @Test
    void shouldIgnoreUnknownMessageTypes() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.forward());
        given(pluginMessageEvent.getIdentifier()).willReturn(VelocityProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSource()).willReturn(sourceConnection);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("unknown-type", "hub"));
        given(player.getUsername()).willReturn("Alice");
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerConnected(new ServerConnectedEvent(player, authServer, null));

        verify(currentServer, never()).sendPluginMessage(any(), any(byte[].class));
    }

    @Test
    void shouldDropSessionWhenPlayerDisconnects() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.forward());
        given(pluginMessageEvent.getIdentifier()).willReturn(VelocityProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSource()).willReturn(sourceConnection);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceConnection.getServer()).willReturn(authServer);
        given(player.getUsername()).willReturn("Alice");
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onDisconnect(new DisconnectEvent(player, DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN));
        bridge.onServerConnected(new ServerConnectedEvent(player, authServer, null));

        verify(currentServer, never()).sendPluginMessage(any(), any(byte[].class));
    }

    @Test
    void shouldRedirectPlayerOnLogoutWhenConfigured() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.forward());
        given(pluginMessageEvent.getIdentifier()).willReturn(VelocityProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSource()).willReturn(sourceConnection);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("logout", "Alice"));
        given(sourceConnection.getServer()).willReturn(nonAuthServer);
        given(nonAuthServer.getServerInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");
        given(proxyServer.getPlayer("alice")).willReturn(Optional.of(player));
        given(proxyServer.getServer("limbo")).willReturn(Optional.of(nonAuthServer));
        given(player.createConnectionRequest(nonAuthServer)).willReturn(connectionRequest);

        VelocityProxyBridge bridge = new VelocityProxyBridge(
            proxyServer, logger, new VelocityProxyConfiguration(Set.of("lobby"), false, true,
                "Authentication required.", true, true, "limbo", true,
                Set.of("/login", "/register"), true, ""),
            new VelocityAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);

        verify(connectionRequest).fireAndForget();
    }

    @Test
    void shouldBlockAndNotForwardClientPluginMessageOnAuthMeChannel() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.forward());
        given(pluginMessageEvent.getIdentifier()).willReturn(VelocityProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSource()).willReturn(player);

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);

        verify(pluginMessageEvent).setResult(PluginMessageEvent.ForwardResult.handled());
    }

    @Test
    void shouldCancelPendingLoginOnExplicitAck() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.forward());
        given(pluginMessageEvent.getIdentifier()).willReturn(VelocityProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSource()).willReturn(sourceConnection);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceConnection.getServer()).willReturn(authServer);
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");
        given(player.getUsername()).willReturn("Alice");
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(currentServer.getServer()).willReturn(authServer);
        given(currentServer.sendPluginMessage(eq(VelocityProxyBridge.AUTHME_CHANNEL), any(byte[].class)))
            .willReturn(true);
        given(proxyServer.getPlayer("alice")).willReturn(Optional.of(player));

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerConnected(new ServerConnectedEvent(player, authServer, null));

        // Now backend sends the explicit ACK
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("perform.login.ack", "Alice"));
        bridge.onPluginMessage(pluginMessageEvent);

        // After ACK, proxyServer.getPlayer should have been called exactly once (by sendAutoLoginIfAlreadySwitched,
        // not by any retry) — the pending login was cancelled before any retry could fire.
        verify(proxyServer, org.mockito.Mockito.times(1)).getPlayer("alice");
    }

    @Test
    void shouldCancelPendingLoginOnImplicitAckFromNonAuthServer() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.forward());
        given(pluginMessageEvent.getIdentifier()).willReturn(VelocityProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSource()).willReturn(sourceConnection);
        given(player.getUsername()).willReturn("Alice");
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");
        given(nonAuthServer.getServerInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");
        given(currentServer.getServer()).willReturn(authServer);
        given(currentServer.sendPluginMessage(eq(VelocityProxyBridge.AUTHME_CHANNEL), any(byte[].class)))
            .willReturn(true);
        given(proxyServer.getPlayer("alice")).willReturn(Optional.of(player));

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());

        // Mark authenticated via auth server login
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceConnection.getServer()).willReturn(authServer);
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerConnected(new ServerConnectedEvent(player, authServer, null));

        // Backend confirms auto-login with a login message from the non-auth server
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceConnection.getServer()).willReturn(nonAuthServer);
        bridge.onPluginMessage(pluginMessageEvent);

        // Pending is now cancelled; proxyServer.getPlayer was called exactly once (by sendAutoLoginIfAlreadySwitched),
        // not again by any retry.
        verify(proxyServer, org.mockito.Mockito.times(1)).getPlayer("alice");
    }

    @Test
    void shouldNotMarkPlayerAuthenticatedIfLoginComesFromNonAuthServer() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.forward());
        given(pluginMessageEvent.getIdentifier()).willReturn(VelocityProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSource()).willReturn(sourceConnection);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceConnection.getServer()).willReturn(nonAuthServer);
        given(nonAuthServer.getServerInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");
        given(player.getUsername()).willReturn("Alice");
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerConnected(new ServerConnectedEvent(player, authServer, null));

        verify(currentServer, never()).sendPluginMessage(any(), any(byte[].class));
    }

    @Test
    void shouldDenySwitchToNonAuthServerForUnauthenticatedPlayer() {
        given(player.getUsername()).willReturn("Alice");
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(nonAuthServer.getServerInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        ServerPreConnectEvent event = new ServerPreConnectEvent(player, nonAuthServer, authServer);

        bridge.onServerPreConnect(event);

        assertFalse(event.getResult().isAllowed());
        verify(player).sendMessage(Component.text("Authentication required.", net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    @Test
    void shouldDisconnectPlayerOnInitialJoinToNonAuthServerWhenSwitchRequiresAuth() {
        given(player.getUsername()).willReturn("Alice");
        given(player.getCurrentServer()).willReturn(Optional.empty());
        given(nonAuthServer.getServerInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        ServerPreConnectEvent event = new ServerPreConnectEvent(player, nonAuthServer);

        bridge.onServerPreConnect(event);

        assertFalse(event.getResult().isAllowed());
        verify(player).disconnect(Component.text("Authentication required.", net.kyori.adventure.text.format.NamedTextColor.RED));
    }

    @Test
    void shouldNotForwardPerformLoginToNonAuthServers() {
        given(pluginMessageEvent.getResult()).willReturn(PluginMessageEvent.ForwardResult.forward());
        given(pluginMessageEvent.getIdentifier()).willReturn(VelocityProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSource()).willReturn(sourceConnection);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceConnection.getServer()).willReturn(authServer);
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");
        given(nonAuthServer.getServerInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerConnected(new ServerConnectedEvent(player, nonAuthServer, null));

        verify(currentServer, never()).sendPluginMessage(any(), any(byte[].class));
    }

    // --- Command blocking tests ---

    @Test
    void shouldBlockNonWhitelistedCommandForUnauthenticatedPlayer() {
        given(commandEvent.getCommandSource()).willReturn(player);
        given(commandEvent.getCommand()).willReturn("spawn");
        given(player.getUsername()).willReturn("alice");
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(currentServer.getServer()).willReturn(authServer);
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onCommandExecute(commandEvent);

        verify(commandEvent).setResult(CommandExecuteEvent.CommandResult.denied());
    }

    @Test
    void shouldAllowWhitelistedCommandForUnauthenticatedPlayer() {
        given(commandEvent.getCommandSource()).willReturn(player);
        given(commandEvent.getCommand()).willReturn("login secret");
        given(player.getUsername()).willReturn("alice");
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(currentServer.getServer()).willReturn(authServer);
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onCommandExecute(commandEvent);

        verify(commandEvent, never()).setResult(any());
    }

    @Test
    void shouldAllowCommandForAuthenticatedPlayer() {
        given(commandEvent.getCommandSource()).willReturn(player);
        given(player.getUsername()).willReturn("alice");
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(currentServer.getServer()).willReturn(authServer);
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        VelocityAuthenticationStore store = new VelocityAuthenticationStore();
        store.markAuthenticated("alice");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), store);
        bridge.onCommandExecute(commandEvent);

        verify(commandEvent, never()).setResult(any());
    }

    @Test
    void shouldAllowCommandIfNotOnAuthServer() {
        given(commandEvent.getCommandSource()).willReturn(player);
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(currentServer.getServer()).willReturn(nonAuthServer);
        given(nonAuthServer.getServerInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onCommandExecute(commandEvent);

        verify(commandEvent, never()).setResult(any());
    }

    @Test
    void shouldAllowCommandIfPlayerHasNoCurrentServer() {
        given(commandEvent.getCommandSource()).willReturn(player);
        given(player.getCurrentServer()).willReturn(Optional.empty());

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onCommandExecute(commandEvent);

        verify(commandEvent, never()).setResult(any());
    }

    @Test
    void shouldAllowCommandIfSourceIsNotAPlayer() {
        given(commandEvent.getCommandSource()).willReturn(consoleSource);

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onCommandExecute(commandEvent);

        verify(commandEvent, never()).setResult(any());
    }

    @Test
    void shouldNotBlockCommandIfCommandsRequireAuthIsDisabled() {
        VelocityProxyConfiguration config = new VelocityProxyConfiguration(
            Set.of("lobby"), false, true, "Authentication required.", false, false, "",
            false, Set.of("/login"), true, "");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, config, new VelocityAuthenticationStore());
        bridge.onCommandExecute(commandEvent);

        verify(commandEvent, never()).setResult(any());
    }

    // --- Chat blocking tests ---

    @Test
    void shouldBlockChatForUnauthenticatedPlayer() {
        given(chatEvent.getPlayer()).willReturn(player);
        given(player.getUsername()).willReturn("alice");
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(currentServer.getServer()).willReturn(authServer);
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPlayerChat(chatEvent);

        verify(chatEvent).setResult(PlayerChatEvent.ChatResult.denied());
    }

    @Test
    void shouldAllowChatForAuthenticatedPlayer() {
        given(chatEvent.getPlayer()).willReturn(player);
        given(player.getUsername()).willReturn("alice");
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(currentServer.getServer()).willReturn(authServer);
        given(authServer.getServerInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        VelocityAuthenticationStore store = new VelocityAuthenticationStore();
        store.markAuthenticated("alice");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), store);
        bridge.onPlayerChat(chatEvent);

        verify(chatEvent, never()).setResult(any());
    }

    @Test
    void shouldAllowChatIfNotOnAuthServer() {
        given(chatEvent.getPlayer()).willReturn(player);
        given(player.getCurrentServer()).willReturn(Optional.of(currentServer));
        given(currentServer.getServer()).willReturn(nonAuthServer);
        given(nonAuthServer.getServerInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPlayerChat(chatEvent);

        verify(chatEvent, never()).setResult(any());
    }

    @Test
    void shouldAllowChatIfPlayerHasNoCurrentServer() {
        given(chatEvent.getPlayer()).willReturn(player);
        given(player.getCurrentServer()).willReturn(Optional.empty());

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, createConfiguration(), new VelocityAuthenticationStore());
        bridge.onPlayerChat(chatEvent);

        verify(chatEvent, never()).setResult(any());
    }

    @Test
    void shouldNotBlockChatIfChatRequiresAuthIsDisabled() {
        VelocityProxyConfiguration config = new VelocityProxyConfiguration(
            Set.of("lobby"), false, true, "Authentication required.", false, false, "",
            true, Set.of("/login"), false, "");

        VelocityProxyBridge bridge = new VelocityProxyBridge(proxyServer, logger, config, new VelocityAuthenticationStore());
        bridge.onPlayerChat(chatEvent);

        verify(chatEvent, never()).setResult(any());
    }

    private static VelocityProxyConfiguration createConfiguration() {
        return new VelocityProxyConfiguration(Set.of("lobby"), false, true,
            "Authentication required.", true, false, "", true,
            Set.of("/login", "/register", "/l", "/reg", "/email", "/captcha", "/2fa", "/totp", "/log"),
            true, "test-secret");
    }

    private static byte[] createAuthMePayload(String typeId, String playerName) {
        ByteArrayDataOutput output = ByteStreams.newDataOutput();
        output.writeUTF(typeId);
        output.writeUTF(playerName.toLowerCase());
        return output.toByteArray();
    }

    private static void assertPerformLoginPayload(byte[] payload, String expectedPlayerName, String sharedSecret) {
        com.google.common.io.ByteArrayDataInput in = ByteStreams.newDataInput(payload);
        assertEquals("perform.login", in.readUTF());
        assertEquals(expectedPlayerName, in.readUTF());
        long timestamp = in.readLong();
        String hmac = in.readUTF();
        assertTrue(Math.abs(System.currentTimeMillis() - timestamp) < 5000L, "timestamp should be recent");
        assertEquals(ProxyMessageSecurity.computeHmac(sharedSecret, expectedPlayerName, timestamp), hmac);
    }
}
