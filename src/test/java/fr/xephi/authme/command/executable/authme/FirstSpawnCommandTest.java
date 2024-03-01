package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link FirstSpawnCommand}.
 */
@ExtendWith(MockitoExtension.class)
class FirstSpawnCommandTest {

    @InjectMocks
    private FirstSpawnCommand command;

    @Mock
    private SpawnLoader spawnLoader;

    @Test
    void shouldTeleportToFirstSpawn() {
        // given
        Location firstSpawn = mock(Location.class);
        given(spawnLoader.getFirstSpawn()).willReturn(firstSpawn);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.emptyList());

        // then
        verify(player).teleport(firstSpawn);
        verify(spawnLoader, atLeastOnce()).getFirstSpawn();
    }

    @Test
    void shouldHandleMissingFirstSpawn() {
        // given
        given(spawnLoader.getFirstSpawn()).willReturn(null);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.emptyList());

        // then
        verify(player).sendMessage(argThat(containsString("spawn has failed")));
        verify(player, never()).teleport(any(Location.class));
    }
}
