package fr.xephi.authme.bungee;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.event.ServerSwitchEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class BungeeProxyBridgeTest {

    @Mock
    private ProxyServer proxyServer;

    @Mock
    private Logger logger;

    @Mock
    private PluginMessageEvent pluginMessageEvent;

    @Mock
    private Server sourceServer;

    @Mock
    private ProxiedPlayer player;

    @Mock
    private Server currentServer;

    @Mock
    private ServerInfo authServerInfo;

    @Mock
    private ServerInfo nonAuthServerInfo;

    @Mock
    private ServerSwitchEvent serverSwitchEvent;

    @Mock
    private PlayerDisconnectEvent playerDisconnectEvent;

    @Mock
    private ChatEvent commandEvent;

    @Mock
    private ChatEvent chatEvent;

    @Mock
    private ServerConnectEvent serverConnectEvent;

    @Captor
    private ArgumentCaptor<byte[]> payloadCaptor;

    @Test
    void shouldTrackAuthenticatedPlayerAndForwardPerformLoginOnServerSwitch() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceServer.getInfo()).willReturn(authServerInfo);
        given(serverSwitchEvent.getPlayer()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerSwitch(serverSwitchEvent);

        verify(authServerInfo).sendData(eq(BungeeProxyBridge.AUTHME_CHANNEL), payloadCaptor.capture(), eq(false));
        assertPerformLoginPayload(payloadCaptor.getValue(), "alice", "test-secret");
    }

    @Test
    void shouldIgnoreUnrelatedPluginMessages() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("unknown-type", "hub"));
        given(serverSwitchEvent.getPlayer()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");
        given(serverSwitchEvent.getFrom()).willReturn(authServerInfo);

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerSwitch(serverSwitchEvent);

        verify(authServerInfo, never()).sendData(any(String.class), any(byte[].class), eq(false));
    }

    @Test
    void shouldDropSessionWhenPlayerDisconnects() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceServer.getInfo()).willReturn(authServerInfo);
        given(playerDisconnectEvent.getPlayer()).willReturn(player);
        given(serverSwitchEvent.getPlayer()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onPlayerDisconnect(playerDisconnectEvent);
        bridge.onServerSwitch(serverSwitchEvent);

        verify(authServerInfo, never()).sendData(any(String.class), any(byte[].class), eq(false));
    }

    @Test
    void shouldRedirectPlayerOnLogoutWhenConfigured() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("logout", "Alice"));
        given(proxyServer.getPlayer("alice")).willReturn(player);
        given(proxyServer.getServerInfo("limbo")).willReturn(nonAuthServerInfo);

        BungeeProxyBridge bridge = new BungeeProxyBridge(
            proxyServer, logger, new BungeeProxyConfiguration(
                Set.of("lobby"), false, true, Set.of("/login"), true, true,
                "Authentication required.", true, true, "limbo", ""),
            new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);

        verify(player).connect(nonAuthServerInfo);
    }

    @Test
    void shouldCancelClientPluginMessageToPreventForwarding() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(player);

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);

        verify(pluginMessageEvent).setCancelled(true);
    }

    @Test
    void shouldCancelPendingLoginOnExplicitAck() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");
        given(serverSwitchEvent.getPlayer()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);
        given(serverSwitchEvent.getFrom()).willReturn(null);

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerSwitch(serverSwitchEvent);

        // Backend sends explicit ACK
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("perform.login.ack", "Alice"));
        bridge.onPluginMessage(pluginMessageEvent);

        // getPlayer called exactly once by sendAutoLoginIfAlreadySwitched (on login), not by any retry
        verify(proxyServer, org.mockito.Mockito.times(1)).getPlayer("alice");
    }

    @Test
    void shouldCancelPendingLoginOnImplicitAckFromNonAuthServer() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(authServerInfo.getName()).willReturn("lobby");
        given(nonAuthServerInfo.getName()).willReturn("survival");
        given(serverSwitchEvent.getPlayer()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());

        // Mark authenticated via auth server login
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceServer.getInfo()).willReturn(authServerInfo);
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerSwitch(serverSwitchEvent);

        // Backend confirms auto-login with login from non-auth server
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceServer.getInfo()).willReturn(nonAuthServerInfo);
        bridge.onPluginMessage(pluginMessageEvent);

        // getPlayer called exactly once by sendAutoLoginIfAlreadySwitched (on login from auth server), not by retries
        verify(proxyServer, org.mockito.Mockito.times(1)).getPlayer("alice");
    }

    @Test
    void shouldNotMarkPlayerAuthenticatedIfLoginComesFromNonAuthServer() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceServer.getInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");
        given(serverSwitchEvent.getPlayer()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerSwitch(serverSwitchEvent);

        verify(authServerInfo, never()).sendData(any(String.class), any(byte[].class), eq(false));
    }

    @Test
    void shouldBlockNonWhitelistedCommandForUnauthenticatedPlayer() {
        given(commandEvent.isCancelled()).willReturn(false);
        given(commandEvent.isCommand()).willReturn(true);
        given(commandEvent.getSender()).willReturn(player);
        given(commandEvent.getMessage()).willReturn("/spawn");
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onCommand(commandEvent);

        verify(commandEvent).setCancelled(true);
    }

    @Test
    void shouldAllowWhitelistedCommandForUnauthenticatedPlayer() {
        given(commandEvent.isCancelled()).willReturn(false);
        given(commandEvent.isCommand()).willReturn(true);
        given(commandEvent.getSender()).willReturn(player);
        given(commandEvent.getMessage()).willReturn("/login secret");
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onCommand(commandEvent);

        verify(commandEvent, never()).setCancelled(true);
    }

    @Test
    void shouldBlockChatForUnauthenticatedPlayer() {
        given(chatEvent.isCancelled()).willReturn(false);
        given(chatEvent.isCommand()).willReturn(false);
        given(chatEvent.getSender()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPlayerChat(chatEvent);

        verify(chatEvent).setCancelled(true);
    }

    @Test
    void shouldDenySwitchToNonAuthServerForUnauthenticatedPlayer() {
        given(serverConnectEvent.isCancelled()).willReturn(false);
        given(serverConnectEvent.getPlayer()).willReturn(player);
        given(serverConnectEvent.getTarget()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPlayerConnectingToServer(serverConnectEvent);

        verify(serverConnectEvent).setCancelled(true);
        verify(player).sendMessage(any(net.md_5.bungee.api.chat.TextComponent.class));
    }

    @Test
    void shouldDisconnectPlayerOnInitialJoinToNonAuthServerWhenSwitchRequiresAuth() {
        given(serverConnectEvent.isCancelled()).willReturn(false);
        given(serverConnectEvent.getPlayer()).willReturn(player);
        given(serverConnectEvent.getTarget()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(null);

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPlayerConnectingToServer(serverConnectEvent);

        verify(serverConnectEvent).setCancelled(true);
        verify(player).disconnect(any(net.md_5.bungee.api.chat.TextComponent.class));
    }

    @Test
    void shouldNotForwardPerformLoginToNonAuthServers() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");
        given(serverSwitchEvent.getPlayer()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerSwitch(serverSwitchEvent);

        verify(nonAuthServerInfo, never()).sendData(any(String.class), any(byte[].class), eq(false));
    }

    @Test
    void shouldForwardPerformLoginWhenLeavingAuthServer() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceServer.getInfo()).willReturn(authServerInfo);
        given(serverSwitchEvent.getPlayer()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");
        given(serverSwitchEvent.getFrom()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);
        bridge.onServerSwitch(serverSwitchEvent);

        verify(nonAuthServerInfo).sendData(eq(BungeeProxyBridge.AUTHME_CHANNEL), payloadCaptor.capture(), eq(false));
        assertPerformLoginPayload(payloadCaptor.getValue(), "alice", "test-secret");
    }

    @Test
    void shouldSendProxyStartedHandshakeToAuthServerOnStartup() {
        given(proxyServer.getServers()).willReturn(Map.of("lobby", authServerInfo));
        given(authServerInfo.getName()).willReturn("lobby");
        given(authServerInfo.getPlayers()).willReturn(List.of(player));

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.broadcastProxyStartedHandshake();

        verify(authServerInfo).sendData(eq(BungeeProxyBridge.AUTHME_CHANNEL), payloadCaptor.capture(), eq(false));
        com.google.common.io.ByteArrayDataInput in = ByteStreams.newDataInput(payloadCaptor.getValue());
        assertEquals("proxy.started", in.readUTF());
        assertEquals("bungee", in.readUTF());
    }

    @Test
    void shouldDeferProxyStartedHandshakeWhenNoPlayersOnAuthServer() {
        given(proxyServer.getServers()).willReturn(Map.of("lobby", authServerInfo));
        given(authServerInfo.getName()).willReturn("lobby");
        given(authServerInfo.getPlayers()).willReturn(List.of());
        given(serverSwitchEvent.getPlayer()).willReturn(player);
        given(player.getName()).willReturn("Alice");
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(authServerInfo);

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.broadcastProxyStartedHandshake();

        // No handshake sent yet (no players at startup)
        verify(authServerInfo, never()).sendData(any(String.class), any(byte[].class), eq(false));

        // First player connection to that auth server triggers the deferred handshake
        given(authServerInfo.getPlayers()).willReturn(List.of(player));
        bridge.onServerSwitch(serverSwitchEvent);

        verify(authServerInfo).sendData(eq(BungeeProxyBridge.AUTHME_CHANNEL), payloadCaptor.capture(), eq(false));
        com.google.common.io.ByteArrayDataInput in = ByteStreams.newDataInput(payloadCaptor.getValue());
        assertEquals("proxy.started", in.readUTF());
        assertEquals("bungee", in.readUTF());
    }

    @Test
    void shouldSendAutoLoginImmediatelyWhenPlayerAlreadySwitchedBeforeLoginMessage() {
        given(pluginMessageEvent.isCancelled()).willReturn(false);
        given(pluginMessageEvent.getTag()).willReturn(BungeeProxyBridge.AUTHME_CHANNEL);
        given(pluginMessageEvent.getSender()).willReturn(sourceServer);
        given(pluginMessageEvent.getData()).willReturn(createAuthMePayload("login", "Alice"));
        given(sourceServer.getInfo()).willReturn(authServerInfo);
        given(authServerInfo.getName()).willReturn("lobby");
        // Player is already on a non-auth server when the login message arrives
        given(proxyServer.getPlayer("alice")).willReturn(player);
        given(player.getServer()).willReturn(currentServer);
        given(currentServer.getInfo()).willReturn(nonAuthServerInfo);
        given(nonAuthServerInfo.getName()).willReturn("survival");

        BungeeProxyBridge bridge = new BungeeProxyBridge(proxyServer, logger, createConfiguration(), new BungeeAuthenticationStore());
        bridge.onPluginMessage(pluginMessageEvent);

        verify(nonAuthServerInfo).sendData(eq(BungeeProxyBridge.AUTHME_CHANNEL), payloadCaptor.capture(), eq(false));
        assertPerformLoginPayload(payloadCaptor.getValue(), "alice", "test-secret");
    }

    private static BungeeProxyConfiguration createConfiguration() {
        return new BungeeProxyConfiguration(
            Set.of("lobby"), false, true, Set.of("/login", "/register", "/l", "/reg", "/email", "/captcha", "/2fa", "/totp", "/log"),
            true, true, "Authentication required.", true, false, "", "test-secret");
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
