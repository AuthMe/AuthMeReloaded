package fr.xephi.authme.service.bungeecord;

import fr.xephi.authme.AuthMe;
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

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class BungeeSenderTest {

    @Mock
    private AuthMe plugin;

    @Mock
    private BukkitService bukkitService;

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
    void shouldRegisterOutgoingChannelsWhenEnabled() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.BUNGEECORD_SERVER)).willReturn("");
        given(messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")).willReturn(false);
        given(messenger.isOutgoingChannelRegistered(plugin, "authme:main")).willReturn(false);

        new BungeeSender(plugin, bukkitService, settings);

        verify(messenger).registerOutgoingPluginChannel(plugin, "BungeeCord");
        verify(messenger).registerOutgoingPluginChannel(plugin, "authme:main");
    }

    @Test
    void shouldUnregisterOutgoingChannelsWhenDisabledOnReload() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true, false);
        given(settings.getProperty(HooksSettings.BUNGEECORD_SERVER)).willReturn("", "");
        given(messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")).willReturn(false, true);
        given(messenger.isOutgoingChannelRegistered(plugin, "authme:main")).willReturn(false, true);

        BungeeSender bungeeSender = new BungeeSender(plugin, bukkitService, settings);
        bungeeSender.reload(settings);

        verify(messenger).registerOutgoingPluginChannel(plugin, "BungeeCord");
        verify(messenger).registerOutgoingPluginChannel(plugin, "authme:main");
        verify(messenger).unregisterOutgoingPluginChannel(plugin, "BungeeCord");
        verify(messenger).unregisterOutgoingPluginChannel(plugin, "authme:main");
    }
}
