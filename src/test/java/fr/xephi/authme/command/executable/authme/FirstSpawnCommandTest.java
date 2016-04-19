package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link FirstSpawnCommand}.
 */
public class FirstSpawnCommandTest {

    @Test
    public void shouldTeleportToFirstSpawn() {
        // given
        Location firstSpawn = mock(Location.class);
        SpawnLoader spawnLoader = mock(SpawnLoader.class);
        given(spawnLoader.getFirstSpawn()).willReturn(firstSpawn);
        CommandService service = mock(CommandService.class);
        given(service.getSpawnLoader()).willReturn(spawnLoader);
        Player player = mock(Player.class);
        ExecutableCommand command = new FirstSpawnCommand();

        // when
        command.executeCommand(player, Collections.<String> emptyList(), service);

        // then
        verify(player).teleport(firstSpawn);
        verify(spawnLoader, atLeastOnce()).getFirstSpawn();
    }

    @Test
    public void shouldHandleMissingFirstSpawn() {
        // given
        SpawnLoader spawnLoader = mock(SpawnLoader.class);
        given(spawnLoader.getFirstSpawn()).willReturn(null);
        CommandService service = mock(CommandService.class);
        given(service.getSpawnLoader()).willReturn(spawnLoader);
        Player player = mock(Player.class);
        ExecutableCommand command = new FirstSpawnCommand();

        // when
        command.executeCommand(player, Collections.<String> emptyList(), service);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(player).sendMessage(captor.capture());
        assertThat(captor.getValue(), containsString("spawn has failed"));
        verify(player, never()).teleport(any(Location.class));
    }
}
