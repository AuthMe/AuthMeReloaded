package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.Test;

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
public class SpawnCommandTest {

    @Test
    public void shouldTeleportToSpawn() {
        // given
        Location spawn = mock(Location.class);
        SpawnLoader spawnLoader = mock(SpawnLoader.class);
        given(spawnLoader.getSpawn()).willReturn(spawn);
        CommandService service = mock(CommandService.class);
        given(service.getSpawnLoader()).willReturn(spawnLoader);
        Player player = mock(Player.class);
        ExecutableCommand command = new SpawnCommand();

        // when
        command.executeCommand(player, Collections.EMPTY_LIST, service);

        // then
        verify(player).teleport(spawn);
        verify(spawnLoader, atLeastOnce()).getSpawn();
    }

    @Test
    public void shouldHandleMissingSpawn() {
        // given
        SpawnLoader spawnLoader = mock(SpawnLoader.class);
        given(spawnLoader.getSpawn()).willReturn(null);
        CommandService service = mock(CommandService.class);
        given(service.getSpawnLoader()).willReturn(spawnLoader);
        Player player = mock(Player.class);
        ExecutableCommand command = new SpawnCommand();

        // when
        command.executeCommand(player, Collections.EMPTY_LIST, service);

        // then
        verify(player).sendMessage(argThat(containsString("Spawn has failed")));
        verify(player, never()).teleport(any(Location.class));
    }
}
