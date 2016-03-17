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
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SetSpawnCommand}.
 */
public class SetSpawnCommandTest {

    @Test
    public void shouldSetSpawn() {
        // given
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        given(player.getLocation()).willReturn(location);

        SpawnLoader spawnLoader = mock(SpawnLoader.class);
        given(spawnLoader.setSpawn(location)).willReturn(true);
        CommandService service = mock(CommandService.class);
        given(service.getSpawnLoader()).willReturn(spawnLoader);

        ExecutableCommand command = new SetSpawnCommand();

        // when
        command.executeCommand(player, Collections.<String> emptyList(), service);

        // then
        verify(spawnLoader).setSpawn(location);
        verify(player).sendMessage(argThat(containsString("defined new spawn")));
    }

    @Test
    public void shouldHandleError() {
        // given
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        given(player.getLocation()).willReturn(location);

        SpawnLoader spawnLoader = mock(SpawnLoader.class);
        given(spawnLoader.setSpawn(location)).willReturn(false);
        CommandService service = mock(CommandService.class);
        given(service.getSpawnLoader()).willReturn(spawnLoader);

        ExecutableCommand command = new SetSpawnCommand();

        // when
        command.executeCommand(player, Collections.<String> emptyList(), service);

        // then
        verify(spawnLoader).setSpawn(location);
        verify(player).sendMessage(argThat(containsString("has failed")));
    }
}
