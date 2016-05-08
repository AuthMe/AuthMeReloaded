package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SpawnCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SpawnCommandTest {

    @InjectMocks
    private SpawnCommand command;

    @Mock
    private SpawnLoader spawnLoader;

    @Mock
    private CommandService service;

    @Test
    public void shouldTeleportToSpawn() {
        // given
        Location spawn = mock(Location.class);
        given(spawnLoader.getSpawn()).willReturn(spawn);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.<String>emptyList(), service);

        // then
        verify(player).teleport(spawn);
        verify(spawnLoader, atLeastOnce()).getSpawn();
    }

    @Test
    public void shouldHandleMissingSpawn() {
        // given
        given(spawnLoader.getSpawn()).willReturn(null);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.<String>emptyList(), service);

        // then
        verify(player).sendMessage(argThat(containsString("Spawn has failed")));
        verify(player, never()).teleport(any(Location.class));
    }
}
