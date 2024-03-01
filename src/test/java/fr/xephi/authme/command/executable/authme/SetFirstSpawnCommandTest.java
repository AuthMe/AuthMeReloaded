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
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link SetFirstSpawnCommand}.
 */
@ExtendWith(MockitoExtension.class)
class SetFirstSpawnCommandTest {

    @InjectMocks
    private SetFirstSpawnCommand command;

    @Mock
    private SpawnLoader spawnLoader;


    @Test
    void shouldSetFirstSpawn() {
        // given
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        given(player.getLocation()).willReturn(location);
        given(spawnLoader.setFirstSpawn(location)).willReturn(true);

        // when
        command.executeCommand(player, Collections.emptyList());

        // then
        verify(spawnLoader).setFirstSpawn(location);
        verify(player).sendMessage(argThat(containsString("defined new first spawn")));
    }

    @Test
    void shouldHandleError() {
        // given
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        given(player.getLocation()).willReturn(location);
        given(spawnLoader.setFirstSpawn(location)).willReturn(false);

        // when
        command.executeCommand(player, Collections.emptyList());

        // then
        verify(spawnLoader).setFirstSpawn(location);
        verify(player).sendMessage(argThat(containsString("has failed")));
    }
}
