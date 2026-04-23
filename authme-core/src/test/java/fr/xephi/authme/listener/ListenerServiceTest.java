package fr.xephi.authme.listener;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link ListenerService}.
 */
@ExtendWith(MockitoExtension.class)
class ListenerServiceTest {

    private ListenerService listenerService;

    @Mock
    private Settings settings;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private ValidationService validationService;

    @BeforeEach
    void setUpMocksAndService() {
        given(settings.getProperty(RegistrationSettings.FORCE)).willReturn(true);
        listenerService = new ListenerService(settings, dataSource, playerCache, validationService);
    }

    @Test
    void shouldHandleEventWithNullEntity() {
        // given
        EntityEvent event = mock(EntityEvent.class);
        given(event.getEntity()).willReturn(null);

        // when
        boolean result = listenerService.shouldCancelEvent(event);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    void shouldHandleEntityEventWithNonPlayerEntity() {
        // given
        EntityEvent event = mock(EntityEvent.class);
        given(event.getEntity()).willReturn(mock(Entity.class));

        // when
        boolean result = listenerService.shouldCancelEvent(event);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    void shouldAllowAuthenticatedPlayer() {
        // given
        String playerName = "Bobby";
        Player player = mockPlayerWithName(playerName);
        given(playerCache.isAuthenticated(playerName)).willReturn(true);
        EntityEvent event = mock(EntityEvent.class);
        given(event.getEntity()).willReturn(player);

        // when
        boolean result = listenerService.shouldCancelEvent(event);

        // then
        assertThat(result, equalTo(false));
        verify(playerCache).isAuthenticated(playerName);
        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldDenyUnLoggedPlayer() {
        // given
        String playerName = "Tester";
        Player player = mockPlayerWithName(playerName);
        given(playerCache.isAuthenticated(playerName)).willReturn(false);
        EntityEvent event = mock(EntityEvent.class);
        given(event.getEntity()).willReturn(player);

        // when
        boolean result = listenerService.shouldCancelEvent(event);

        // then
        assertThat(result, equalTo(true));
        verify(playerCache).isAuthenticated(playerName);
        // makes sure the setting is checked first = avoid unnecessary DB operation
        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldAllowUnloggedPlayerForOptionalRegistration() {
        // given
        String playerName = "myPlayer1";
        Player player = mockPlayerWithName(playerName);
        given(playerCache.isAuthenticated(playerName)).willReturn(false);
        given(settings.getProperty(RegistrationSettings.FORCE)).willReturn(false);
        EntityEvent event = mock(EntityEvent.class);
        given(event.getEntity()).willReturn(player);
        listenerService.reload(settings);

        // when
        boolean result = listenerService.shouldCancelEvent(event);

        // then
        assertThat(result, equalTo(false));
        verify(playerCache).isAuthenticated(playerName);
        verify(dataSource).isAuthAvailable(playerName);
    }

    @Test
    void shouldAllowUnrestrictedName() {
        // given
        String playerName = "Npc2";
        Player player = mockPlayerWithName(playerName);
        EntityEvent event = mock(EntityEvent.class);
        given(event.getEntity()).willReturn(player);
        given(validationService.isUnrestricted(playerName)).willReturn(true);

        // when
        boolean result = listenerService.shouldCancelEvent(event);

        // then
        assertThat(result, equalTo(false));
        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldAllowNpcPlayer() {
        // given
        String playerName = "other_npc";
        Player player = mockPlayerWithName(playerName);
        EntityEvent event = mock(EntityEvent.class);
        given(event.getEntity()).willReturn(player);
        given(player.hasMetadata("NPC")).willReturn(true);

        // when
        boolean result = listenerService.shouldCancelEvent(event);

        // then
        assertThat(result, equalTo(false));
        verify(player).hasMetadata("NPC");
    }

    @Test
    // This simply forwards to shouldCancelEvent(Player), so the rest is already tested
    void shouldHandlePlayerEvent() {
        // given
        String playerName = "example";
        Player player = mockPlayerWithName(playerName);
        PlayerEvent event = new TestPlayerEvent(player);
        given(playerCache.isAuthenticated(playerName)).willReturn(true);

        // when
        boolean result = listenerService.shouldCancelEvent(event);

        // then
        assertThat(result, equalTo(false));
        verify(playerCache).isAuthenticated(playerName);
        verifyNoInteractions(dataSource);
    }

    @Test
    void shouldHandlePlayerEventWithNullPlayer() {
        // given
        PlayerEvent event = new TestPlayerEvent(null);

        // when
        boolean result = listenerService.shouldCancelEvent(event);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    // The previous tests verify most of shouldCancelEvent(Player)
    void shouldVerifyBasedOnPlayer() {
        // given
        String playerName = "player";
        Player player = mockPlayerWithName(playerName);

        // when
        boolean result = listenerService.shouldCancelEvent(player);

        // then
        assertThat(result, equalTo(true));
        verify(playerCache).isAuthenticated(playerName);
        verifyNoInteractions(dataSource);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

    /**
     * Test implementation of {@link PlayerEvent}.
     */
    private static final class TestPlayerEvent extends PlayerEvent {
        TestPlayerEvent(Player player) {
            super(player);
        }

        @Override
        public HandlerList getHandlers() {
            return null;
        }
    }
}
