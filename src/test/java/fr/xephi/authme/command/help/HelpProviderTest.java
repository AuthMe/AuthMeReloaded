package fr.xephi.authme.command.help;

import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.FoundResultStatus;
import fr.xephi.authme.command.TestCommandsUtil;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.internal.stubbing.answers.ReturnsArgumentAt;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static fr.xephi.authme.command.TestCommandsUtil.getCommandWithLabel;
import static fr.xephi.authme.command.help.HelpProvider.ALL_OPTIONS;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_ALTERNATIVES;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_ARGUMENTS;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_CHILDREN;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_COMMAND;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_DESCRIPTION;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_LONG_DESCRIPTION;
import static fr.xephi.authme.command.help.HelpProvider.SHOW_PERMISSIONS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link HelpProvider}.
 */
@ExtendWith(MockitoExtension.class)
class HelpProviderTest {

    private static Collection<CommandDescription> commands;

    private HelpProvider helpProvider;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private HelpMessagesService helpMessagesService;
    @Mock
    private CommandSender sender;

    @BeforeAll
    static void setUpCommands() {
        commands = TestCommandsUtil.generateCommands();
    }

    @BeforeEach
    void initializeHelpProvider() {
        setDefaultHelpMessages(helpMessagesService);
        helpProvider = new HelpProvider(permissionsManager, helpMessagesService);
    }

    @Test
    void shouldShowLongDescription() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "login");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "login"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_COMMAND | SHOW_LONG_DESCRIPTION | SHOW_DESCRIPTION);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(5));
        assertThat(lines.get(0), containsString("Header"));
        assertThat(lines.get(1), containsString("Command: /authme login <password>"));
        assertThat(lines.get(2), containsString("Short description: login cmd"));
        assertThat(lines.get(3), equalTo("Detailed description:"));
        assertThat(lines.get(4), containsString("'login' test command"));
    }

    @Test
    void shouldShowArguments() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "reg"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_ARGUMENTS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(4));
        assertThat(lines.get(0), containsString("Header"));
        assertThat(lines.get(1), equalTo("Arguments:"));
        assertThat(lines.get(2), containsString("password: 'password' argument description"));
        assertThat(lines.get(3), containsString("confirmation: 'confirmation' argument description"));
    }

    @Test
    void shouldShowSpecifyIfArgumentIsOptional() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "email");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("email"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_ARGUMENTS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(3));
        assertThat(lines.get(2), containsString("player: 'player' argument description (Optional)"));
    }

    /** Verifies that the "Arguments:" line is not shown if the command has no arguments. */
    @Test
    void shouldNotShowAnythingIfCommandHasNoArguments() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_ARGUMENTS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1)); // only has the help banner
    }

    @Test
    void shouldShowAndEvaluatePermissions() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "unregister");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("unreg"));
        given(sender.isOp()).willReturn(true);
        given(permissionsManager.hasPermission(sender, AdminPermission.UNREGISTER)).willReturn(true);
        given(permissionsManager.hasPermission(sender, command.getPermission())).willReturn(true);

        // when
        helpProvider.outputHelp(sender, result, SHOW_PERMISSIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(5));
        assertThat(lines.get(1), containsString("Permissions:"));
        assertThat(lines.get(2),
            containsString(AdminPermission.UNREGISTER.getNode() + " (Has permission)"));
        assertThat(lines.get(3), containsString("Default: Op only (Has permission)"));
        assertThat(lines.get(4), containsString("Result: Has permission"));
    }

    @Test
    void shouldShowAndEvaluateForbiddenPermissions() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "unregister");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("unregister"));
        given(sender.isOp()).willReturn(false);
        given(permissionsManager.hasPermission(sender, AdminPermission.UNREGISTER)).willReturn(false);
        given(permissionsManager.hasPermission(sender, command.getPermission())).willReturn(false);

        // when
        helpProvider.outputHelp(sender, result, SHOW_PERMISSIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(5));
        assertThat(lines.get(1), containsString("Permissions:"));
        assertThat(lines.get(2),
            containsString(AdminPermission.UNREGISTER.getNode() + " (No permission)"));
        assertThat(lines.get(3), containsString("Default: Op only (No permission)"));
        assertThat(lines.get(4), containsString("Result: No permission"));
    }

    @Test
    void shouldNotShowAnythingForEmptyPermissions() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_PERMISSIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
    }

    @Test
    void shouldNotShowAnythingForNullPermissionsOnCommand() {
        // given
        CommandDescription command = mock(CommandDescription.class);
        given(command.getPermission()).willReturn(null);
        given(command.getLabels()).willReturn(Collections.singletonList("test"));
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("test"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_PERMISSIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
    }

    @Test
    void shouldShowAlternatives() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "reg"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_ALTERNATIVES);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(4));
        assertThat(lines.get(1), containsString("Alternatives:"));
        assertThat(lines.get(2), containsString("/authme register <password> <confirmation>"));
        assertThat(lines.get(3), containsString("/authme r <password> <confirmation>"));
    }

    @Test
    void shouldNotShowAnythingIfHasNoAlternatives() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "login");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "login"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_ALTERNATIVES);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
    }

    @Test
    void shouldShowChildren() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));
        given(helpMessagesService.getDescription(getCommandWithLabel(commands, "authme", "login")))
            .willReturn("Command for login [localized]");
        given(helpMessagesService.getDescription(getCommandWithLabel(commands, "authme", "register")))
            .willReturn("Registration command [localized]");

        // when
        helpProvider.outputHelp(sender, result, SHOW_CHILDREN);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(4));
        assertThat(lines.get(1), equalTo("Children:"));
        assertThat(lines.get(2), equalTo(" /authme login: Command for login [localized]"));
        assertThat(lines.get(3), equalTo(" /authme register: Registration command [localized]"));
    }

    @Test
    void shouldNotShowCommandsTitleForCommandWithNoChildren() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_CHILDREN);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
    }

    @Test
    void shouldHandleUnboundFoundCommandResult() {
        // given
        FoundCommandResult result = new FoundCommandResult(null, Arrays.asList("authme", "test"),
            Collections.emptyList(), 0.0, FoundResultStatus.UNKNOWN_LABEL);

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
    void shouldShowCommandSyntaxWithCorrectLabels() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Arrays.asList("authme", "ragister"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_COMMAND);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(2));
        assertThat(lines.get(0), containsString("Header"));
        assertThat(lines.get(1), containsString("Command: /authme register <password> <confirmation>"));
    }

    @Test
    void shouldRetainCorrectLabels() {
        // given
        List<String> labels = Arrays.asList("authme", "reg");
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");

        // when
        List<String> result = HelpProvider.filterCorrectLabels(command, labels);

        // then
        assertThat(result, equalTo(labels));
    }

    @Test
    void shouldReplaceIncorrectLabels() {
        // given
        List<String> labels = Arrays.asList("authme", "wrong");
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");

        // when
        List<String> result = HelpProvider.filterCorrectLabels(command, labels);

        // then
        assertThat(result, contains("authme", "register"));
    }

    @Test
    void shouldDisableSectionsWithEmptyTranslations() {
        // given
        given(helpMessagesService.getMessage(HelpSection.DETAILED_DESCRIPTION)).willReturn("");
        given(helpMessagesService.getMessage(HelpSection.ALTERNATIVES)).willReturn("");
        given(helpMessagesService.getMessage(HelpSection.PERMISSIONS)).willReturn("");

        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, ALL_OPTIONS);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(6));
        assertThat(lines.get(0), equalTo("Header"));
        assertThat(lines.get(1), equalTo("Command: /authme register <password> <confirmation>"));
        assertThat(lines.get(2), equalTo("Short description: register cmd"));
        assertThat(lines.get(3), equalTo("Arguments:"));
        assertThat(lines.get(4), containsString("'password' argument description"));
        assertThat(lines.get(5), containsString("'confirmation' argument description"));
    }

    @Test
    void shouldNotReturnAnythingForAllDisabledSections() {
        // given
        given(helpMessagesService.getMessage(HelpSection.COMMAND)).willReturn("");
        given(helpMessagesService.getMessage(HelpSection.ALTERNATIVES)).willReturn("");
        given(helpMessagesService.getMessage(HelpSection.PERMISSIONS)).willReturn("");

        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_COMMAND | SHOW_ALTERNATIVES | SHOW_PERMISSIONS);

        // then
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    void shouldSkipEmptyHeader() {
        // given
        given(helpMessagesService.getMessage(HelpMessage.HEADER)).willReturn("");
        CommandDescription command = getCommandWithLabel(commands, "authme", "register");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("authme"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_COMMAND);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(1));
        assertThat(lines.get(0), equalTo("Command: /authme register <password> <confirmation>"));
    }

    @Test
    void shouldShowAlternativesForRootCommand() {
        // given
        CommandDescription command = getCommandWithLabel(commands, "unregister");
        FoundCommandResult result = newFoundResult(command, Collections.singletonList("unreg"));

        // when
        helpProvider.outputHelp(sender, result, SHOW_COMMAND | SHOW_ALTERNATIVES);

        // then
        List<String> lines = getLines(sender);
        assertThat(lines, hasSize(4));
        assertThat(lines.get(0), equalTo("Header"));
        assertThat(lines.get(1), equalTo("Command: /unreg <player>"));
        assertThat(lines.get(2), equalTo("Alternatives:"));
        assertThat(lines.get(3), equalTo(" /unregister <player>"));
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
        return new FoundCommandResult(command, labels, Collections.emptyList(), 0.0, FoundResultStatus.SUCCESS);
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
        return captor.getAllValues().stream().map(s -> removeColors(s)).collect(Collectors.toList());
    }

    private static void setDefaultHelpMessages(HelpMessagesService helpMessagesService) {
        lenient().when(helpMessagesService.buildLocalizedDescription(any(CommandDescription.class)))
            .thenAnswer(new ReturnsArgumentAt(0));
        for (HelpMessage key : HelpMessage.values()) {
            String text = key.name().replace("_", " ").toLowerCase(Locale.ROOT);
            lenient().when(helpMessagesService.getMessage(key))
                .thenReturn(text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1));
        }
        for (DefaultPermission permission : DefaultPermission.values()) {
            String text = permission.name().replace("_", " ").toLowerCase(Locale.ROOT);
            lenient().when(helpMessagesService.getMessage(permission))
                .thenReturn(text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1));
        }
        for (HelpSection section : HelpSection.values()) {
            String text = section.name().replace("_", " ").toLowerCase(Locale.ROOT);
            lenient().when(helpMessagesService.getMessage(section))
                .thenReturn(text.substring(0, 1).toUpperCase(Locale.ROOT) + text.substring(1));
        }
    }

}
