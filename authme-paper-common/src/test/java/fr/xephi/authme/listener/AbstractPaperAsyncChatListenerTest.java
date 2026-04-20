package fr.xephi.authme.listener;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.chat.SignedMessage;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class AbstractPaperAsyncChatListenerTest {

    @InjectMocks
    private TestAsyncChatListener listener;

    @Mock
    private Settings settings;
    @Mock
    private Messages messages;
    @Mock
    private ListenerService listenerService;
    @Mock
    private PermissionsManager permissionsManager;

    @Test
    public void shouldAllowChatWhenGlobalChatIsEnabled() {
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(true);
        AsyncChatEvent event = newAsyncChatEvent(mock(Player.class), new HashSet<>());

        listener.onPlayerChat(event);

        verifyNoInteractions(listenerService, messages);
        assertThat(event.isCancelled(), is(false));
    }

    @Test
    public void shouldCancelChatForUnauthenticatedPlayer() {
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        Player player = mock(Player.class);
        AsyncChatEvent event = newAsyncChatEvent(player, new HashSet<>());
        given(listenerService.shouldCancelEvent(player)).willReturn(true);
        given(permissionsManager.hasPermission(player, PlayerStatePermission.ALLOW_CHAT_BEFORE_LOGIN)).willReturn(false);

        listener.onPlayerChat(event);

        assertThat(event.isCancelled(), is(true));
        verify(messages).send(player, MessageKey.DENIED_CHAT);
    }

    @Test
    public void shouldAllowChatForPlayerWithBypassPermission() {
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        given(settings.getProperty(RestrictionSettings.HIDE_CHAT)).willReturn(false);
        Player player = mock(Player.class);
        AsyncChatEvent event = newAsyncChatEvent(player, new HashSet<>());
        given(listenerService.shouldCancelEvent(player)).willReturn(true);
        given(permissionsManager.hasPermission(player, PlayerStatePermission.ALLOW_CHAT_BEFORE_LOGIN)).willReturn(true);

        listener.onPlayerChat(event);

        assertThat(event.isCancelled(), is(false));
        verifyNoInteractions(messages);
    }

    @Test
    public void shouldFilterUnauthenticatedViewersWhenHideChatEnabled() {
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        given(settings.getProperty(RestrictionSettings.HIDE_CHAT)).willReturn(true);
        Player sender = mock(Player.class);
        Player unauthViewer = mock(Player.class);
        Player authViewer = mock(Player.class);
        Set<Audience> viewers = new HashSet<>();
        viewers.add(unauthViewer);
        viewers.add(authViewer);
        AsyncChatEvent event = newAsyncChatEvent(sender, viewers);
        given(listenerService.shouldCancelEvent(sender)).willReturn(false);
        given(listenerService.shouldCancelEvent(unauthViewer)).willReturn(true);
        given(listenerService.shouldCancelEvent(authViewer)).willReturn(false);

        listener.onPlayerChat(event);

        assertThat(viewers, contains(authViewer));
        assertThat(event.isCancelled(), is(false));
    }

    @Test
    public void shouldCancelEventWhenAllViewersFiltered() {
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        given(settings.getProperty(RestrictionSettings.HIDE_CHAT)).willReturn(true);
        Player sender = mock(Player.class);
        Player unauthViewer = mock(Player.class);
        Set<Audience> viewers = new HashSet<>();
        viewers.add(unauthViewer);
        AsyncChatEvent event = newAsyncChatEvent(sender, viewers);
        given(listenerService.shouldCancelEvent(sender)).willReturn(false);
        given(listenerService.shouldCancelEvent(unauthViewer)).willReturn(true);

        listener.onPlayerChat(event);

        assertThat(viewers, empty());
        assertThat(event.isCancelled(), is(true));
    }

    private static AsyncChatEvent newAsyncChatEvent(Player player, Set<Audience> viewers) {
        return new AsyncChatEvent(true, player, viewers,
            (source, sourceDisplayName, message, viewer) -> message,
            Component.text("test"), Component.text("test"),
            mock(SignedMessage.class));
    }

    private static final class TestAsyncChatListener extends AbstractPaperAsyncChatListener {
    }
}
