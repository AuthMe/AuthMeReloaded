package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SetFirstSpawnCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class SetFirstSpawnCommandTest {

    @InjectMocks
    private SetFirstSpawnCommand command;

    @Mock
    private SpawnLoader spawnLoader;

    @Mock
    private Messages messages;

    @Test
    public void shouldSetFirstSpawn() {
        // given
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        given(player.getLocation()).willReturn(location);
        given(spawnLoader.setFirstSpawn(location)).willReturn(true);

        // when
        command.executeCommand(player, Collections.emptyList());

        // then
        verify(spawnLoader).setFirstSpawn(location);
        verify(messages).send(player, MessageKey.FIRST_SPAWN_SET_SUCCESS);
    }

    @Test
    public void shouldHandleError() {
        // given
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        given(player.getLocation()).willReturn(location);
        given(spawnLoader.setFirstSpawn(location)).willReturn(false);

        // when
        command.executeCommand(player, Collections.emptyList());

        // then
        verify(spawnLoader).setFirstSpawn(location);
        verify(messages).send(player, MessageKey.FIRST_SPAWN_SET_FAIL);
    }
}
