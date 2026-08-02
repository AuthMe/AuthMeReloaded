package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.ProxyLoginRequestValidator;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.UUID;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskAsynchronously;
import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncEntityTaskFromOptionallyAsyncTask;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class BungeeReceiverTest {

    @Mock
    private AuthMe plugin;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private ProxySessionManager proxySessionManager;

    @Mock
    private Management management;

    @Mock
    private BungeeSender bungeeSender;

    @Mock
    private DataSource dataSource;

    @Mock
    private ProxyLoginRequestValidator proxyLoginRequestValidator;

    @Mock
    private Settings settings;

    @Mock
    private Server server;

    @Mock
    private Messenger messenger;

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @BeforeEach
    void setUp() {
        given(plugin.getServer()).willReturn(server);
        given(server.getMessenger()).willReturn(messenger);
    }

    @Test
    void shouldRegisterIncomingChannelWhenEnabled() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false);

        new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource,
            proxyLoginRequestValidator, settings);

        verify(messenger).registerIncomingPluginChannel(eq(plugin), eq("authme:main"), any(BungeeReceiver.class));
    }

    @Test
    void shouldUnregisterIncomingChannelWhenDisabledOnReload() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true, false);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false, true);

        BungeeReceiver bungeeReceiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource,
                proxyLoginRequestValidator, settings);
        bungeeReceiver.reload(settings);

        verify(messenger).registerIncomingPluginChannel(plugin, "authme:main", bungeeReceiver);
        verify(messenger).unregisterIncomingPluginChannel(plugin, "authme:main", bungeeReceiver);
    }

    @Test
    void shouldQueueSessionAndForceLoginWhenPerformLoginReceivedForOnlinePlayer() {
        // given
        String sharedSecret = "test-secret";
        String playerName = "Bobby";
        long timestamp = System.currentTimeMillis();
        String hmac = HashUtils.hmacSha256(sharedSecret, playerName + ":" + timestamp + ":");

        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.PROXY_SHARED_SECRET)).willReturn(sharedSecret);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false);

        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);
        given(proxyLoginRequestValidator.validate(player, null)).willReturn(true);

        BungeeReceiver receiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource,
                proxyLoginRequestValidator, settings);

        byte[] payload = buildPerformLoginPayload(playerName, timestamp, hmac);

        // when
        receiver.onPluginMessageReceived("authme:main", player, payload);

        // then
        verify(proxySessionManager).processProxySessionMessage(playerName, null);
        verify(management).forceLoginFromProxy(player);
        verify(bungeeSender).sendAuthMeBungeecordMessage(player, MessageType.PERFORM_LOGIN_ACK);
    }

    @Test
    void shouldOnlyQueueSessionWhenPerformLoginReceivedForOfflinePlayer() {
        // given
        String sharedSecret = "test-secret";
        String playerName = "Bobby";
        long timestamp = System.currentTimeMillis();
        String hmac = HashUtils.hmacSha256(sharedSecret, playerName + ":" + timestamp + ":");

        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.PROXY_SHARED_SECRET)).willReturn(sharedSecret);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false);
        given(bukkitService.getPlayerExact(playerName)).willReturn(null);

        BungeeReceiver receiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource,
                proxyLoginRequestValidator, settings);

        Player carrier = mock(Player.class);
        byte[] payload = buildPerformLoginPayload(playerName, timestamp, hmac);

        // when
        receiver.onPluginMessageReceived("authme:main", carrier, payload);

        // then
        verify(proxySessionManager).processProxySessionMessage(playerName, null);
        verify(management, never()).forceLoginFromProxy(any());
        verify(bungeeSender, never()).sendAuthMeBungeecordMessage(any(), any());
    }

    @Test
    void shouldFinalizePremiumLoginWhenValidateAcceptsVerifiedUuid() {
        // given
        String sharedSecret = "test-secret";
        String playerName = "Bobby";
        UUID verifiedUuid = UUID.fromString("8d6d0684-d8b4-4d40-8d2d-0dd4df5555c8");
        long timestamp = System.currentTimeMillis();
        String hmac = HashUtils.hmacSha256(sharedSecret, playerName + ":" + timestamp + ":" + verifiedUuid);

        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.PROXY_SHARED_SECRET)).willReturn(sharedSecret);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);
        setBukkitServiceToScheduleSyncEntityTaskFromOptionallyAsyncTask(bukkitService);

        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);
        given(proxyLoginRequestValidator.validate(player, verifiedUuid)).willReturn(true);

        BungeeReceiver receiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource,
                proxyLoginRequestValidator, settings);

        byte[] payload = buildPerformLoginPayload(playerName, timestamp, verifiedUuid.toString(), hmac);

        // when
        receiver.onPluginMessageReceived("authme:main", player, payload);

        // then
        verify(proxySessionManager).processProxySessionMessage(playerName, verifiedUuid);
        verify(management).forceLoginFromProxy(player);
        verify(bungeeSender).sendAuthMeBungeecordMessage(player, MessageType.PERFORM_LOGIN_ACK);
    }

    @Test
    void shouldRemoveQueuedRequestWhenPremiumValidateRejects() {
        // given
        String sharedSecret = "test-secret";
        String playerName = "Bobby";
        UUID verifiedUuid = UUID.fromString("8d6d0684-d8b4-4d40-8d2d-0dd4df5555c8");
        long timestamp = System.currentTimeMillis();
        String hmac = HashUtils.hmacSha256(sharedSecret, playerName + ":" + timestamp + ":" + verifiedUuid);

        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.PROXY_SHARED_SECRET)).willReturn(sharedSecret);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);
        given(proxyLoginRequestValidator.validate(player, verifiedUuid)).willReturn(false);

        BungeeReceiver receiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource,
                proxyLoginRequestValidator, settings);

        byte[] payload = buildPerformLoginPayload(playerName, timestamp, verifiedUuid.toString(), hmac);

        // when
        receiver.onPluginMessageReceived("authme:main", player, payload);

        // then
        verify(proxySessionManager).removeLoginRequest(playerName);
        verify(management, never()).forceLoginFromProxy(any());
        verify(bungeeSender, never()).sendAuthMeBungeecordMessage(any(), any());
    }

    @Test
    void shouldValidateAndQueueConfigPhasePerformLogin() {
        // given
        String sharedSecret = "test-secret";
        String playerName = "Bobby";
        long timestamp = System.currentTimeMillis();
        String hmac = HashUtils.hmacSha256(sharedSecret, playerName + ":" + timestamp + ":");

        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.PROXY_SHARED_SECRET)).willReturn(sharedSecret);

        BungeeReceiver receiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource,
                proxyLoginRequestValidator, settings);

        byte[] payload = buildPerformLoginPayload(playerName, timestamp, hmac);

        // when
        String result = receiver.handleConfigPhasePerformLogin(payload);

        // then
        assertThat(result, equalTo("bobby"));
        verify(proxySessionManager).processProxySessionMessage(playerName, null);
        verify(management, never()).forceLoginFromProxy(any());
    }

    @Test
    void shouldRejectConfigPhasePerformLoginWithInvalidHmac() {
        // given
        String sharedSecret = "test-secret";
        String playerName = "Bobby";
        long timestamp = System.currentTimeMillis();

        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.PROXY_SHARED_SECRET)).willReturn(sharedSecret);

        BungeeReceiver receiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource,
                proxyLoginRequestValidator, settings);

        byte[] payload = buildPerformLoginPayload(playerName, timestamp, "not-a-valid-hmac");

        // when
        String result = receiver.handleConfigPhasePerformLogin(payload);

        // then
        assertThat(result, nullValue());
        verify(proxySessionManager, never()).processProxySessionMessage(any(), any());
    }

    private static byte[] buildPerformLoginPayload(String playerName, long timestamp, String hmac) {
        return buildPerformLoginPayload(playerName, timestamp, "", hmac);
    }

    private static byte[] buildPerformLoginPayload(String playerName, long timestamp, String uuid, String hmac) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageType.PERFORM_LOGIN.getId());
        out.writeUTF(playerName);
        out.writeLong(timestamp);
        out.writeUTF(uuid);
        out.writeUTF(hmac);
        return out.toByteArray();
    }
}
