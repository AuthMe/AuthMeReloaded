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
 * Test for {@link SetFirstSpawnCommand}.
 */
public class SetFirstSpawnCommandTest {

    @Test
    public void shouldSetFirstSpawn() {
        // given
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        given(player.getLocation()).willReturn(location);

        SpawnLoader spawnLoader = mock(SpawnLoader.class);
        given(spawnLoader.setFirstSpawn(location)).willReturn(true);
        CommandService service = mock(CommandService.class);
        given(service.getSpawnLoader()).willReturn(spawnLoader);

        ExecutableCommand command = new SetFirstSpawnCommand();

        // when
        command.executeCommand(player, Collections.EMPTY_LIST, service);

        // then
        verify(spawnLoader).setFirstSpawn(location);
        verify(player).sendMessage(argThat(containsString("defined new first spawn")));
    }

    @Test
    public void shouldHandleError() {
        // given
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        given(player.getLocation()).willReturn(location);

        SpawnLoader spawnLoader = mock(SpawnLoader.class);
        given(spawnLoader.setFirstSpawn(location)).willReturn(false);
        CommandService service = mock(CommandService.class);
        given(service.getSpawnLoader()).willReturn(spawnLoader);

        ExecutableCommand command = new SetFirstSpawnCommand();

        // when
        command.executeCommand(player, Collections.EMPTY_LIST, service);

        // then
        verify(spawnLoader).setFirstSpawn(location);
        verify(player).sendMessage(argThat(containsString("has failed")));
    }
}
