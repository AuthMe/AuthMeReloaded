package fr.xephi.authme.command;

import org.bukkit.ChatColor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for {@link CommandUtils}.
 */
class CommandUtilsTest {

    private static Collection<CommandDescription> commands;

    @BeforeAll
    static void setUpTestCommands() {
        commands = Collections.unmodifiableCollection(TestCommandsUtil.generateCommands());
    }

    @Test
    void shouldReturnCommandPath() {
        // given
        CommandDescription base = CommandDescription.builder()
            .labels("authme", "auth")
            .description("Base")
            .detailedDescription("Test base command.")
            .executableCommand(ExecutableCommand.class)
            .register();
        CommandDescription command = CommandDescription.builder()
            .parent(base)
            .labels("help", "h", "?")
            .description("Child")
            .detailedDescription("Test child command.")
            .executableCommand(ExecutableCommand.class)
            .register();

        // when
        String commandPath = CommandUtils.constructCommandPath(command);

        // then
        assertThat(commandPath, equalTo("/authme help"));
    }

    @Test
    void shouldComputeMinAndMaxOnEmptyCommand() {
        // given
        CommandDescription command = getBuilderForArgsTest().register();

        // when / then
        checkArgumentCount(command, 0, 0);
    }

    @Test
    void shouldComputeMinAndMaxOnCommandWithMandatoryArgs() {
        // given
        CommandDescription command = getBuilderForArgsTest()
            .withArgument("Test", "Arg description", false)
            .withArgument("Test22", "Arg description 2", false)
            .register();

        // when / then
        checkArgumentCount(command, 2, 2);
    }

    @Test
    void shouldComputeMinAndMaxOnCommandIncludingOptionalArgs() {
        // given
        CommandDescription command = getBuilderForArgsTest()
            .withArgument("arg1", "Arg description", false)
            .withArgument("arg2", "Arg description 2", true)
            .withArgument("arg3", "Arg description 3", true)
            .register();

        // when / then
        checkArgumentCount(command, 1, 3);
    }

    @Test
    void shouldFormatSimpleArgument() {
        // given
        CommandDescription command = TestCommandsUtil.getCommandWithLabel(commands, "authme");
        List<String> labels = Collections.singletonList("authme");

        // when
        String result = CommandUtils.buildSyntax(command, labels);

        // then
        assertThat(result, equalTo(ChatColor.WHITE + "/authme" + ChatColor.YELLOW));
    }

    @Test
    void shouldFormatCommandWithMultipleArguments() {
        // given
        CommandDescription command = TestCommandsUtil.getCommandWithLabel(commands, "authme", "register");
        List<String> labels = Arrays.asList("authme", "reg");

        // when
        String result = CommandUtils.buildSyntax(command, labels);

        // then
        assertThat(result, equalTo(ChatColor.WHITE + "/authme" + ChatColor.YELLOW + " reg <password> <confirmation>"));
    }


    @Test
    void shouldFormatCommandWithOptionalArgument() {
        // given
        CommandDescription command = TestCommandsUtil.getCommandWithLabel(commands, "email");
        List<String> labels = Collections.singletonList("email");

        // when
        String result = CommandUtils.buildSyntax(command, labels);

        // then
        assertThat(result, equalTo(ChatColor.WHITE + "/email" + ChatColor.YELLOW + " [player]"));
    }


    private static void checkArgumentCount(CommandDescription command, int expectedMin, int expectedMax) {
        assertThat(CommandUtils.getMinNumberOfArguments(command), equalTo(expectedMin));
        assertThat(CommandUtils.getMaxNumberOfArguments(command), equalTo(expectedMax));
    }

    private static CommandDescription.CommandBuilder getBuilderForArgsTest() {
        return CommandDescription.builder()
            .labels("authme", "auth")
            .description("Base")
            .detailedDescription("Test base command.")
            .executableCommand(ExecutableCommand.class);
    }
}
