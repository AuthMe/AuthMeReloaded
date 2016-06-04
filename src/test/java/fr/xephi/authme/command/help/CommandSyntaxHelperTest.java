package fr.xephi.authme.command.help;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.TestCommandsUtil;
import org.bukkit.ChatColor;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link CommandSyntaxHelper}.
 */
public class CommandSyntaxHelperTest {

    private static Set<CommandDescription> commands;

    @BeforeClass
    public static void setUpTestCommands() {
        commands = TestCommandsUtil.generateCommands();
    }

    @Test
    public void shouldFormatSimpleArgument() {
        // given
        CommandDescription command = TestCommandsUtil.getCommandWithLabel(commands, "authme");
        List<String> labels = Collections.singletonList("authme");

        // when
        String result = CommandSyntaxHelper.getSyntax(command, labels);

        // then
        assertThat(result, equalTo(ChatColor.WHITE + "/authme" + ChatColor.YELLOW));
    }

    @Test
    public void shouldFormatCommandWithMultipleArguments() {
        // given
        CommandDescription command = TestCommandsUtil.getCommandWithLabel(commands, "authme", "register");
        List<String> labels = Arrays.asList("authme", "reg");

        // when
        String result = CommandSyntaxHelper.getSyntax(command, labels);

        // then
        assertThat(result, equalTo(ChatColor.WHITE + "/authme" + ChatColor.YELLOW + " reg <password> <confirmation>"));
    }


    @Test
    public void shouldFormatCommandWithOptionalArgument() {
        // given
        CommandDescription command = TestCommandsUtil.getCommandWithLabel(commands, "email");
        List<String> labels = Collections.singletonList("email");

        // when
        String result = CommandSyntaxHelper.getSyntax(command, labels);

        // then
        assertThat(result, equalTo(ChatColor.WHITE + "/email" + ChatColor.YELLOW + " [player]"));
    }

    @Test
    public void shouldHaveHiddenConstructor() {
        // given / when / then
        TestHelper.validateHasOnlyPrivateEmptyConstructor(CommandSyntaxHelper.class);
    }

}
