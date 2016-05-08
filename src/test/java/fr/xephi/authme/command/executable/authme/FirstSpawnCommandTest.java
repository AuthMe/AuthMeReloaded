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
 * Test for {@link FirstSpawnCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class FirstSpawnCommandTest {

    @InjectMocks
    private FirstSpawnCommand command;

    @Mock
    private SpawnLoader spawnLoader;

    @Mock
    private CommandService service;

    @Test
    public void shouldTeleportToFirstSpawn() {
        // given
        Location firstSpawn = mock(Location.class);
        given(spawnLoader.getFirstSpawn()).willReturn(firstSpawn);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.<String>emptyList(), service);

        // then
        verify(player).teleport(firstSpawn);
        verify(spawnLoader, atLeastOnce()).getFirstSpawn();
    }

    @Test
    public void shouldHandleMissingFirstSpawn() {
        // given
        given(spawnLoader.getFirstSpawn()).willReturn(null);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.<String>emptyList(), service);

        // then
        verify(player).sendMessage(argThat(containsString("spawn has failed")));
        verify(player, never()).teleport(any(Location.class));
    }
}
