package fr.xephi.authme.listener;

import fr.xephi.authme.service.TeleportationService;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.spigotmc.event.player.PlayerSpawnLocationEvent;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LegacyPlayerSpawnLocationListenerTest {

    @InjectMocks
    private LegacyPlayerSpawnLocationListener listener;

    @Mock
    private TeleportationService teleportationService;

    @Test
    void shouldSetCustomSpawnLocationForLegacySpawnEvent() {
        // given
        Player player = mock(Player.class);
        Location originalSpawn = mock(Location.class);
        Location customSpawn = mock(Location.class);
        PlayerSpawnLocationEvent event = spy(new PlayerSpawnLocationEvent(player, originalSpawn));
        given(teleportationService.prepareOnJoinSpawnLocation(player, originalSpawn)).willReturn(customSpawn);

        // when
        listener.onPlayerSpawn(event);

        // then
        verify(teleportationService).prepareOnJoinSpawnLocation(player, originalSpawn);
        verify(event).setSpawnLocation(customSpawn);
    }

    @Test
    void shouldLeaveOriginalSpawnWhenNoCustomSpawnIsProvided() {
        // given
        Player player = mock(Player.class);
        Location originalSpawn = mock(Location.class);
        PlayerSpawnLocationEvent event = spy(new PlayerSpawnLocationEvent(player, originalSpawn));

        // when
        listener.onPlayerSpawn(event);

        // then
        verify(teleportationService).prepareOnJoinSpawnLocation(player, originalSpawn);
        verify(event, never()).setSpawnLocation(any(Location.class));
    }
}
