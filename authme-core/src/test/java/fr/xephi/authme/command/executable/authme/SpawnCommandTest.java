package fr.xephi.authme.command.executable.authme;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link SpawnCommand}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class SpawnCommandTest {

    @InjectMocks
    private SpawnCommand command;

    @Mock
    private SpawnLoader spawnLoader;

    @Mock
    private Messages messages;

    @Test
    public void shouldTeleportToSpawn() {
        // given
        Location spawn = mock(Location.class);
        given(spawnLoader.getSpawn()).willReturn(spawn);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.emptyList());

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
        command.executeCommand(player, Collections.emptyList());

        // then
        verify(messages).send(player, MessageKey.SPAWN_NOT_DEFINED);
        verify(player, never()).teleport(any(Location.class));
    }
}


