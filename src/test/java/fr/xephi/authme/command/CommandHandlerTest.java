package fr.xephi.authme.command;

import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.command.CommandSender;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link CommandHandler}.
 */
public class CommandHandlerTest {

    private static List<CommandDescription> commands;
    private static CommandHandler handler;

    @BeforeClass
    public static void setUpCommandHandler() {
        WrapperMock.createInstance();

        CommandDescription authMeBase = createCommand(null, null, singletonList("authme"));
        createCommand(PlayerPermission.LOGIN, authMeBase, singletonList("login"), newArgument("password", false));
        createCommand(PlayerPermission.LOGIN, authMeBase, asList("register", "reg"),
            newArgument("password", false), newArgument("confirmation", false));

        CommandDescription testBase = createCommand(null, null, singletonList("test"), newArgument("test", true));
        commands = asList(authMeBase, testBase);
        handler = new CommandHandler(commands);
    }

    @Test
    public void shouldForwardCommandToExecutable() {
        // given
        CommandSender sender = Mockito.mock(CommandSender.class);
        given(sender.isOp()).willReturn(true);
        String bukkitLabel = "authme";
        String[] args = {"login", "password"};

        // when
        handler.processCommand(sender, bukkitLabel, args);

        // then
        final CommandDescription loginCmd = commands.get(0).getChildren().get(0);
        verify(sender, never()).sendMessage(anyString());
        verify(loginCmd.getExecutableCommand()).executeCommand(
            eq(sender), any(CommandParts.class), any(CommandParts.class));
    }

    @Test
    @Ignore // TODO ljacqu Fix test --> command classes too tightly coupled at the moment
    public void shouldRejectCommandWithTooManyArguments() {
        // given
        CommandSender sender = Mockito.mock(CommandSender.class);
        given(sender.isOp()).willReturn(true);
        String bukkitLabel = "authme";
        String[] args = {"login", "password", "__unneededArgument__"};

        // when
        boolean result = handler.processCommand(sender, bukkitLabel, args);

        // then
        assertThat(result, equalTo(true));
        final CommandDescription loginCmd = commands.get(0).getChildren().get(0);
        assertSenderGotMessageContaining("help", sender);
        verify(loginCmd.getExecutableCommand()).executeCommand(
            eq(sender), any(CommandParts.class), any(CommandParts.class));
    }

    private static CommandDescription createCommand(PlayerPermission permission, CommandDescription parent,
                                                    List<String> labels, CommandArgumentDescription... arguments) {
        CommandDescription.CommandBuilder command = CommandDescription.builder()
            .labels(labels)
            .parent(parent)
            .permissions(CommandPermissions.DefaultPermission.OP_ONLY, permission)
            .description("Test")
            .detailedDescription("Test command")
            .executableCommand(mock(ExecutableCommand.class));

        if (arguments != null && arguments.length > 0) {
            for (CommandArgumentDescription argument : arguments) {
                command.withArgument(argument.getLabel(), "Test description", argument.isOptional());
            }
        }

        return command.build();
    }

    private static CommandArgumentDescription newArgument(String label, boolean isOptional) {
        return new CommandArgumentDescription(label, "Test description", isOptional);
    }

    private void assertSenderGotMessageContaining(String text, CommandSender sender) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender).sendMessage(captor.capture());
        assertThat(captor.getValue(), stringContainsInOrder(text));
    }
}
