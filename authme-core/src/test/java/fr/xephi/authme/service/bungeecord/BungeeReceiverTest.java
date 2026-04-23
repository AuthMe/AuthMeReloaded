package fr.xephi.authme.service.bungeecord;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.Server;
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

        new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, settings);

        verify(messenger).registerIncomingPluginChannel(eq(plugin), eq("authme:main"), any(BungeeReceiver.class));
    }

    @Test
    void shouldUnregisterIncomingChannelWhenDisabledOnReload() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true, false);
        given(messenger.isIncomingChannelRegistered(plugin, "authme:main")).willReturn(false, true);

        BungeeReceiver bungeeReceiver =
            new BungeeReceiver(plugin, bukkitService, proxySessionManager, management, bungeeSender, settings);
        bungeeReceiver.reload(settings);

        verify(messenger).registerIncomingPluginChannel(plugin, "authme:main", bungeeReceiver);
        verify(messenger).unregisterIncomingPluginChannel(plugin, "authme:main", bungeeReceiver);
    }
}
