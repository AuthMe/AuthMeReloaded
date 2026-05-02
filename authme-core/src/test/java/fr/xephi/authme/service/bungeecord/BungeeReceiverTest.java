package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PremiumService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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
    private PendingPremiumCache pendingPremiumCache;

    @Mock
    private PremiumService premiumService;

    @Mock
    private Settings settings;

    @Mock
    private Server server;

    @Mock
    private Messenger messenger;

    @BeforeEach
    void setUp() {
        given(plugin.getServer()).willReturn(server);
        given(server.getMessenger()).willReturn(messenger);
    }

    @Test
    void shouldRegisterIncomingChannelWhenEnabled() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false);

        new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource, pendingPremiumCache, premiumService, settings);

        verify(messenger).registerIncomingPluginChannel(eq(plugin), eq("authme:main"), any(BungeeReceiver.class));
    }

    @Test
    void shouldUnregisterIncomingChannelWhenDisabledOnReload() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true, false);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false, true);

        BungeeReceiver bungeeReceiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource, pendingPremiumCache, premiumService, settings);
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
        String hmac = HashUtils.hmacSha256(sharedSecret, playerName + ":" + timestamp);

        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.PROXY_SHARED_SECRET)).willReturn(sharedSecret);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false);

        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);

        BungeeReceiver receiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource, pendingPremiumCache, premiumService, settings);

        byte[] payload = buildPerformLoginPayload(playerName, timestamp, hmac);

        // when
        receiver.onPluginMessageReceived("authme:main", player, payload);

        // then
        verify(proxySessionManager).processProxySessionMessage(playerName);
        verify(management).forceLoginFromProxy(player);
        verify(bungeeSender).sendAuthMeBungeecordMessage(player, MessageType.PERFORM_LOGIN_ACK);
    }

    @Test
    void shouldOnlyQueueSessionWhenPerformLoginReceivedForOfflinePlayer() {
        // given
        String sharedSecret = "test-secret";
        String playerName = "Bobby";
        long timestamp = System.currentTimeMillis();
        String hmac = HashUtils.hmacSha256(sharedSecret, playerName + ":" + timestamp);

        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.PROXY_SHARED_SECRET)).willReturn(sharedSecret);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false);
        given(bukkitService.getPlayerExact(playerName)).willReturn(null);

        BungeeReceiver receiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, dataSource, pendingPremiumCache, premiumService, settings);

        Player carrier = mock(Player.class);
        byte[] payload = buildPerformLoginPayload(playerName, timestamp, hmac);

        // when
        receiver.onPluginMessageReceived("authme:main", carrier, payload);

        // then
        verify(proxySessionManager).processProxySessionMessage(playerName);
        verify(management, never()).forceLoginFromProxy(any());
        verify(bungeeSender, never()).sendAuthMeBungeecordMessage(any(), any());
    }

    private static byte[] buildPerformLoginPayload(String playerName, long timestamp, String hmac) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageType.PERFORM_LOGIN.getId());
        out.writeUTF(playerName);
        out.writeLong(timestamp);
        out.writeUTF(hmac);
        return out.toByteArray();
    }
}
