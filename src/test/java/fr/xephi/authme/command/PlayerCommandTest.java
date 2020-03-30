package fr.xephi.authme.command;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link PlayerCommand}.
 */
class PlayerCommandTest {

    @Test
    void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        PlayerCommandImpl command = new PlayerCommandImpl();

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(sender).sendMessage(argThat(containsString("only for players")));
    }

    @Test
    void shouldCallRunCommandForPlayer() {
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
    void shouldRejectNonPlayerAndSendAlternative() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerCommandWithAlt command = new PlayerCommandWithAlt();

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(sender, times(1)).sendMessage(argThat(containsString("use /authme test <command> instead")));
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
