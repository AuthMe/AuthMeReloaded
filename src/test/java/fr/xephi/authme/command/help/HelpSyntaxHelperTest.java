package fr.xephi.authme.command.help;

import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.executable.authme.AuthMeCommand;
import fr.xephi.authme.command.executable.authme.RegisterCommand;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static java.util.Collections.singletonList;
import static org.bukkit.ChatColor.BOLD;
import static org.bukkit.ChatColor.ITALIC;
import static org.bukkit.ChatColor.WHITE;
import static org.bukkit.ChatColor.YELLOW;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link HelpSyntaxHelper}.
 */
public class HelpSyntaxHelperTest {

    @Test
    public void shouldFormatSimpleCommand() {
        // given
        CommandDescription description = getDescription();

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(
                description, new CommandParts(), "", false);

        // then
        assertThat(result, equalTo(WHITE + "/authme register" + ITALIC + " [name]"));
    }

    @Test
    public void shouldFormatSimpleCommandWithOptionalParam() {
        // given
        CommandDescription description = getDescription();
        description.setArguments(singletonList(
                new CommandArgumentDescription("test", "", false)));

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(
                description, new CommandParts(), null, false);

        // then
        assertThat(result, equalTo(WHITE + "/authme register" + ITALIC + " <test>"));
    }

    @Test
    public void shouldFormatCommandWithMultipleParams() {
        // given
        CommandDescription description = getDescription();
        description.setArguments(Arrays.asList(
                new CommandArgumentDescription("name", "", true),
                new CommandArgumentDescription("test", "", false)));

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(
                description, new CommandParts(), "", false);

        // then
        assertThat(result, equalTo(WHITE + "/authme register" + ITALIC + " [name]" + ITALIC + " <test>"));
    }

    @Test
    public void shouldHighlightCommandWithMultipleParams() {
        // given
        CommandDescription description = getDescription();
        description.setArguments(Arrays.asList(
                new CommandArgumentDescription("name", "", true),
                new CommandArgumentDescription("test", "", false)));

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(
                description, new CommandParts(), "", true);

        // then
        assertThat(result, equalTo(WHITE + "/authme "
                + YELLOW + BOLD + "register"
                + YELLOW + ITALIC + " [name]" + ITALIC + " <test>"));
    }

    @Test
    public void shouldHighlightCommandWithNoParams() {
        // given
        CommandDescription description = getDescription();
        description.setArguments(new ArrayList<CommandArgumentDescription>());

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(
                description, new CommandParts(), null, true);

        // then
        assertThat(result, equalTo(WHITE + "/authme " + YELLOW + BOLD + "register" + YELLOW));
    }

    @Test
    public void shouldFormatSimpleCommandWithAlternativeLabel() {
        // given
        CommandDescription description = getDescription();

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(
                description, new CommandParts(), "alt", false);

        // then
        assertThat(result, equalTo(WHITE + "/authme alt" + ITALIC + " [name]"));
    }

    @Test
    public void shouldHighlightCommandWithAltLabelAndUnlimitedArguments() {
        // given
        CommandDescription description = getDescription();
        description.setArguments(Arrays.asList(
            new CommandArgumentDescription("name", "", true),
            new CommandArgumentDescription("test", "", false)));
        description.setMaximumArguments(false);

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(
                description, new CommandParts(), "test", true);

        // then
        assertThat(result, equalTo(WHITE + "/authme "
                + YELLOW + BOLD + "test"
                + YELLOW + ITALIC + " [name]" + ITALIC + " <test>" + ITALIC + " ..."));
    }


    private static CommandDescription getDescription() {
        CommandDescription base = new CommandDescription(new AuthMeCommand(),
                singletonList("authme"),
                "Base command",
                "AuthMe base command",
                null);
        CommandArgumentDescription userArg = new CommandArgumentDescription(
                "name", "", true);

        return new CommandDescription(
                new RegisterCommand(),
                Arrays.asList("register", "r"),
                "Register a player",
                "Register the specified player with the specified password.",
                base,
                singletonList(userArg));
    }
}
