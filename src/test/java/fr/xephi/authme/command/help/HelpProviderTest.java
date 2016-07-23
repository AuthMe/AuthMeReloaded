package fr.xephi.authme.command.help;

import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.FoundResultStatus;
import fr.xephi.authme.command.TestCommandsUtil;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static fr.xephi.authme.command.TestCommandsUtil.getCommandWithLabel;
import static fr.xephi.authme.command.help.HelpProvider.ALL_OPTIONS;
import static fr.xephi.authme.command.help.HelpProvider.HIDE_COMMAND;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_ALTERNATIVES;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_ARGUMENTS;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_CHILDREN;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_LONG_DESCRIPTION;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_PERMISSIONS;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link HelpProvider}.
 */
public class HelpProviderTest {

    private static final String HELP_HEADER = "Help";
    private static Set<CommandDescription> commands;
    private HelpProvider helpProvider;
    private PermissionsManager permissionsManager;
    private CommandSender sender;

    @BeforeClass
    public static void setUpCommands() {
        commands = TestCommandsUtil.generateCommands();
    }

    @Before
    public void setUpHelpProvider() {
        permissionsManager = mock(PermissionsManager.class);
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.HELP_HEADER)).willReturn(HELP_HEADER);
        helpProvider = new HelpProvider(permissionsManager, settings);
        sender = mock(CommandSender.class);
    }

    @Test
    public void shouldShowLongDescription() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "login");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "login"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_LONG_DESCRIPTION);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(5));
        assertThat(lines.get(0), containsString(HELP_HEADER + " HELP"));
        assertThat(removeColors(lines.get(1)), containsString("Command: /authme login <password>"));
        assertThat(removeColors(lines.get(2)), containsString("Short description: login cmd"));
        assertThat(removeColors(lines.get(3)), equalTo("Detailed description:"));
        assertThat(removeColors(lines.get(4)), containsString("'login' test command"));
    }

    @Test
    public void shouldShowArguments() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "reg"));

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_ARGUMENTS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(4));
        assertThat(lines.get(0), containsString(HELP_HEADER + " HELP"));
        assertThat(removeColors(lines.get(1)), equalTo("Arguments:"));
        assertThat(removeColors(lines.get(2)), containsString("password: 'password' argument description"));
        assertThat(removeColors(lines.get(3)), containsString("confirmation: 'confirmation' argument description"));
    }

    @Test
    public void shouldShowSpecifyIfArgumentIsOptional() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "email");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("email"));

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_ARGUMENTS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(3));
        assertThat(removeColors(lines.get(2)), containsString("player: 'player' argument description (Optional)"));
    }

    /** Verifies that the "Arguments:" line is not shown if the command has no arguments. */
    @Test
    public void shouldNotShowAnythingIfCommandHasNoArguments() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_ARGUMENTS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1)); // only has the help banner
    }

    @Test
    public void shouldShowAndEvaluatePermissions() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "unregister");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("unreg"));
        given(sender.isOp()).willReturn(true);
        given(permissionsManager.hasPermission(sender, AdminPermission.UNREGISTER)).willReturn(true);
        given(permissionsManager.hasPermission(sender, command.getPermission())).willReturn(true);

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_PERMISSIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(5));
        assertThat(removeColors(lines.get(1)), containsString("Permissions:"));
        assertThat(removeColors(lines.get(2)),
            containsString(AdminPermission.UNREGISTER.getNode() + " (You have permission)"));
        assertThat(removeColors(lines.get(3)), containsString("Default: OP's only (You have permission)"));
        assertThat(removeColors(lines.get(4)), containsString("Result: You have permission"));
    }

    @Test
    public void shouldShowAndEvaluateForbiddenPermissions() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "unregister");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("unregister"));
        given(sender.isOp()).willReturn(false);
        given(permissionsManager.hasPermission(sender, AdminPermission.UNREGISTER)).willReturn(false);
        given(permissionsManager.hasPermission(sender, command.getPermission())).willReturn(false);

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_PERMISSIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(5));
        assertThat(removeColors(lines.get(1)), containsString("Permissions:"));
        assertThat(removeColors(lines.get(2)),
            containsString(AdminPermission.UNREGISTER.getNode() + " (No permission)"));
        assertThat(removeColors(lines.get(3)), containsString("Default: OP's only (No permission)"));
        assertThat(removeColors(lines.get(4)), containsString("Result: No permission"));
    }

    @Test
    public void shouldNotShowAnythingForEmptyPermissions() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_PERMISSIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
    }

    @Test
    public void shouldNotShowAnythingForNullPermissionsOnCommand() {
        // given
        CommandDescription command = mock(CommandDescription.class);
        given(command.getPermission()).willReturn(null);
        given(command.getLabels()).willReturn(Collections.singletonList("test"));
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("test"));

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_PERMISSIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
    }

    @Test
    public void shouldShowAlternatives() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "reg"));

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_ALTERNATIVES);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(4));
        assertThat(removeColors(lines.get(1)), containsString("Alternatives:"));
        assertThat(removeColors(lines.get(2)), containsString("/authme register <password> <confirmation>"));
        assertThat(removeColors(lines.get(3)), containsString("/authme r <password> <confirmation>"));
    }

    @Test
    public void shouldNotShowAnythingIfHasNoAlternatives() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "login");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "login"));

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_ALTERNATIVES);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
    }

    @Test
    public void shouldShowChildren() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_CHILDREN);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(4));
        assertThat(removeColors(lines.get(1)), containsString("Commands:"));
        assertThat(removeColors(lines.get(2)), containsString("/authme login: login cmd"));
        assertThat(removeColors(lines.get(3)), containsString("/authme register: register cmd"));
    }

    @Test
    public void shouldNotShowCommandsTitleForCommandWithNoChildren() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, HIDE_COMMAND | SHOW_CHILDREN);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
    }

    @Test
    public void shouldHandleUnboundFoundCommandResult() {
        // given
        FoundCommandResult result = new FoundCommandResult(null, Arrays.asList("authme", "test"),
            Collections.<String>emptyList(), 0.0, FoundResultStatus.UNKNOWN_LABEL);

        // when
        helpProvider.outputHelp(sender, result, ALL_OPTIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
        assertThat(lines.get(0), containsString("Failed to retrieve any help information"));
    }

    /**
     * Since command parts may be mapped to a command description with labels that don't completely correspond to it,
     * (e.g. suggest "register command" for /authme ragister), we need to check the labels and construct a correct list
     */
    @Test
    public void shouldShowCommandSyntaxWithCorrectLabels() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "ragister"));

        // when
        helpProvider.outputHelp(sender, result, 0);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(2));
        assertThat(lines.get(0), containsString(HELP_HEADER + " HELP"));
        assertThat(removeColors(lines.get(1)), containsString("Command: /authme register <password> <confirmation>"));
    }

    @Test
    public void shouldRetainCorrectLabels() {
        // given
        List<String> labels = Arrays.asList("authme", "reg");
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");

        // when
        List<String> result = HelpProvider.filterCorrectLabels(command, labels);

        // then
        assertThat(result, equalTo(labels));
    }

    @Test
    public void shouldReplaceIncorrectLabels() {
        // given
        List<String> labels = Arrays.asList("authme", "wrong");
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");

        // when
        List<String> result = HelpProvider.filterCorrectLabels(command, labels);

        // then
        assertThat(result, contains("authme", "register"));
    }

    /**
     * Generate an instance of {@link FoundCommandResult} with the given command and labels. All other fields aren't
     * retrieved by {@link HelpProvider} and so are initialized to default values for the tests.
     *
     * @param command The command description
     * @param labels The labels of the command (as in a real use case, they do not have to be correct)
     * @return The generated FoundCommandResult object
     */
    private static FoundCommandResult newFoundResult(CommandDescription command, List<String> labels) {
        return new FoundCommandResult(command, labels, Collections.<String>emptyList(), 0.0, FoundResultStatus.SUCCESS);
    }

    private static String removeColors(String str) {
        for (ChatColor color : ChatColor.values()) {
            str = str.replace(color.toString(), "");
        }
        return str;
    }
    
    private static List<String> getLines(CommandSender sender) {
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, atLeastOnce()).sendMessage(captor.capture());
        return captor.getAllValues();
    }

}
