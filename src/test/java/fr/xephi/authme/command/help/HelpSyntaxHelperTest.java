package fr.xephi.authme.command.help;

import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.executable.authme.RegisterAdminCommand;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

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
        CommandDescription description = getDescriptionBuilder()
            .withArgument("name", "The name", true)
            .build();

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(description, newParts(), "", false);

        // then
        assertThat(result, equalTo(WHITE + "/authme register" + ITALIC + " [name]"));
    }

    @Test
    public void shouldFormatSimpleCommandWithOptionalParam() {
        // given
        CommandDescription description = getDescriptionBuilder()
            .withArgument("test", "", false)
            .build();

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(description, newParts(), null, false);

        // then
        assertThat(result, equalTo(WHITE + "/authme register" + ITALIC + " <test>"));
    }

    @Test
    public void shouldFormatCommandWithMultipleParams() {
        // given
        CommandDescription description = getDescriptionBuilder()
            .withArgument("name", "", true)
            .withArgument("test", "", false)
            .build();

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(description, newParts(), "", false);

        // then
        assertThat(result, equalTo(WHITE + "/authme register" + ITALIC + " [name]" + ITALIC + " <test>"));
    }

    @Test
    public void shouldHighlightCommandWithMultipleParams() {
        // given
        CommandDescription description = getDescriptionBuilder()
            .withArgument("name", "", true)
            .withArgument("test", "", false)
            .build();

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(description, newParts(), "", true);

        // then
        assertThat(result, equalTo(WHITE + "/authme "
                + YELLOW + BOLD + "register"
                + YELLOW + ITALIC + " [name]" + ITALIC + " <test>"));
    }

    @Test
    public void shouldHighlightCommandWithNoParams() {
        // given
        CommandDescription description = getDescriptionBuilder().build();

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(description, newParts(), null, true);

        // then
        assertThat(result, equalTo(WHITE + "/authme " + YELLOW + BOLD + "register" + YELLOW));
    }

    @Test
    public void shouldFormatSimpleCommandWithAlternativeLabel() {
        // given
        CommandDescription description = getDescriptionBuilder()
            .withArgument("name", "The name", true)
            .build();

        // when
        String result = HelpSyntaxHelper.getCommandSyntax(description, newParts(), "alt", false);

        // then
        assertThat(result, equalTo(WHITE + "/authme alt" + ITALIC + " [name]"));
    }

    private static CommandParts newParts() {
        // TODO ljacqu 20151204: Remove this method once CommandParts has been removed
        return new CommandParts(new ArrayList<String>());
    }

    private static CommandDescription.CommandBuilder getDescriptionBuilder() {
        CommandDescription base = CommandDescription.builder()
            .labels("authme")
            .description("Base command")
            .detailedDescription("AuthMe base command")
            .parent(null)
            .executableCommand(Mockito.mock(ExecutableCommand.class))
            .build();

        return CommandDescription.builder()
            .executableCommand(Mockito.mock(RegisterAdminCommand.class))
            .labels("register", "r")
            .description("Register a player")
            .detailedDescription("Register the specified player with the specified password.")
            .parent(base)
            .executableCommand(Mockito.mock(ExecutableCommand.class));
    }
}
