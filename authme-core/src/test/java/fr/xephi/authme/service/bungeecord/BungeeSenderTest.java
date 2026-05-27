package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
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

    @Mock
    private Player carrier;

    @Captor
    private ArgumentCaptor<byte[]> payloadCaptor;

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

    @Test
    void shouldSendSingleChunkForEmptyPremiumList() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.BUNGEECORD_SERVER)).willReturn("");
        given(messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")).willReturn(true);
        given(messenger.isOutgoingChannelRegistered(plugin, "authme:main")).willReturn(true);
        given(plugin.isEnabled()).willReturn(true);

        BungeeSender sender = new BungeeSender(plugin, bukkitService, settings);
        sender.sendPremiumList(carrier, List.of());

        verify(bukkitService).sendAuthMePluginMessage(eq(carrier), payloadCaptor.capture());
        ByteArrayDataInput in = ByteStreams.newDataInput(payloadCaptor.getValue());
        assertEquals("premium.list.chunk", in.readUTF());
        assertEquals("0:1:", in.readUTF());
    }

    @Test
    void shouldSendSingleChunkForSmallPremiumList() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.BUNGEECORD_SERVER)).willReturn("");
        given(messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")).willReturn(true);
        given(messenger.isOutgoingChannelRegistered(plugin, "authme:main")).willReturn(true);
        given(plugin.isEnabled()).willReturn(true);
        List<String> names = List.of("alice", "bob", "charlie");

        BungeeSender sender = new BungeeSender(plugin, bukkitService, settings);
        sender.sendPremiumList(carrier, names);

        verify(bukkitService).sendAuthMePluginMessage(eq(carrier), payloadCaptor.capture());
        ByteArrayDataInput in = ByteStreams.newDataInput(payloadCaptor.getValue());
        assertEquals("premium.list.chunk", in.readUTF());
        String field = in.readUTF();
        assertTrue(field.startsWith("0:1:"), "Expected single last chunk but got: " + field);
    }

    @Test
    void shouldNormalizePremiumListUsernamesToLowercase() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.BUNGEECORD_SERVER)).willReturn("");
        given(messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")).willReturn(true);
        given(messenger.isOutgoingChannelRegistered(plugin, "authme:main")).willReturn(true);
        given(plugin.isEnabled()).willReturn(true);

        BungeeSender sender = new BungeeSender(plugin, bukkitService, settings);
        sender.sendPremiumList(carrier, List.of("Alice", "BOB"));

        verify(bukkitService).sendAuthMePluginMessage(eq(carrier), payloadCaptor.capture());
        ByteArrayDataInput in = ByteStreams.newDataInput(payloadCaptor.getValue());
        assertEquals("premium.list.chunk", in.readUTF());
        assertEquals("0:1:alice,bob", in.readUTF());
    }

    @Test
    void shouldSendTwoChunksFor1001Names() {
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(true);
        given(settings.getProperty(HooksSettings.BUNGEECORD_SERVER)).willReturn("");
        given(messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")).willReturn(true);
        given(messenger.isOutgoingChannelRegistered(plugin, "authme:main")).willReturn(true);
        given(plugin.isEnabled()).willReturn(true);
        List<String> names = IntStream.range(0, 1001)
            .mapToObj(i -> "player" + i)
            .collect(Collectors.toList());

        BungeeSender sender = new BungeeSender(plugin, bukkitService, settings);
        sender.sendPremiumList(carrier, names);

        verify(bukkitService, times(2)).sendAuthMePluginMessage(eq(carrier), payloadCaptor.capture());
        List<byte[]> payloads = payloadCaptor.getAllValues();
        ByteArrayDataInput in0 = ByteStreams.newDataInput(payloads.get(0));
        assertEquals("premium.list.chunk", in0.readUTF());
        assertTrue(in0.readUTF().startsWith("0:0:"), "First chunk should not be last");
        ByteArrayDataInput in1 = ByteStreams.newDataInput(payloads.get(1));
        assertEquals("premium.list.chunk", in1.readUTF());
        assertTrue(in1.readUTF().startsWith("1:1:"), "Second chunk should be last");
    }
}
