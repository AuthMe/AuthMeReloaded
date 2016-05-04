package fr.xephi.authme.command;

import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PlayerCommand}.
 */
public class PlayerCommandTest {

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        PlayerCommandImpl command = new PlayerCommandImpl();

        // when
        command.executeCommand(sender, Collections.<String>emptyList(), mock(CommandService.class));

        // then
        verify(sender).sendMessage(argThat(containsString("only for players")));
    }

    @Test
    public void shouldCallRunCommandForPlayer() {
        // given
        Player player = mock(Player.class);
        List<String> arguments = Arrays.asList("arg1", "testarg2");
        CommandService service = mock(CommandService.class);
        PlayerCommandImpl command = new PlayerCommandImpl();

        // when
        command.executeCommand(player, arguments, service);

        // then
        verify(player, times(1)).sendMessage("testarg2");
    }

    @Test
    public void shouldRejectNonPlayerAndSendAlternative() {
        // given
        CommandSender sender = mock(CommandSender.class);
        PlayerCommandWithAlt command = new PlayerCommandWithAlt();

        // when
        command.executeCommand(sender, Collections.<String>emptyList(), mock(CommandService.class));

        // then
        verify(sender, times(1)).sendMessage(argThat(containsString("use /authme test <command> instead")));
    }


    private static class PlayerCommandImpl extends PlayerCommand {
        @Override
        public void runCommand(Player player, List<String> arguments, CommandService commandService) {
            player.sendMessage(arguments.get(1));
        }
    }

    private static class PlayerCommandWithAlt extends PlayerCommand {
        @Override
        public void runCommand(Player player, List<String> arguments, CommandService commandService) {
            throw new IllegalStateException("Should not be called");
        }
        @Override
        public String getAlternativeCommand() {
            return "/authme test <command>";
        }
    }
}
