package fr.xephi.authme.command;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PlayerCommand}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class PlayerCommandTest {

    @Mock
    private Messages messages;

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        PlayerCommandImpl command = new PlayerCommandImpl();
        command.messages = messages;

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(messages).send(sender, MessageKey.PLAYER_COMMAND_ONLY);
    }

    @Test
    public void shouldCallRunCommandForPlayer() {
        // given
        Player player = mock(Player.class);
        List<String> arguments = Arrays.asList("arg1", "testarg2");
        PlayerCommandImpl command = new PlayerCommandImpl();

        // when
        command.executeCommand(player, arguments);

        // then
        verify(player, times(1)).sendMessage("testarg2");
    }

    @Test
    public void shouldRejectNonPlayerAndSendAlternative() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerCommandWithAlt command = new PlayerCommandWithAlt();
        command.messages = messages;

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(messages).send(eq(sender), eq(MessageKey.PLAYER_COMMAND_ONLY_WITH_ALTERNATIVE),
            eq("/authme test <command>"));
    }


    private static class PlayerCommandImpl extends PlayerCommand {
        @Override
        public void runCommand(Player player, List<String> arguments) {
            player.sendMessage(arguments.get(1));
        }
    }

    private static class PlayerCommandWithAlt extends PlayerCommand {
        @Override
        public void runCommand(Player player, List<String> arguments) {
            throw new IllegalStateException("Should not be called");
        }
        @Override
        public String getAlternativeCommand() {
            return "/authme test <command>";
        }
    }
}


