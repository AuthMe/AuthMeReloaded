package fr.xephi.authme.output;

import com.google.common.collect.Lists;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandInitializer;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link LogFilterHelper}.
 */
public class LogFilterHelperTest {

    private static final List<CommandDescription> ALL_COMMANDS = new CommandInitializer().getCommands();

    /**
     * Checks that {@link LogFilterHelper#COMMANDS_TO_SKIP} contains the entries we expect
     * (commands with password argument).
     */
    @Test
    public void shouldBlacklistAllSensitiveCommands() {
        // given
        List<CommandDescription> sensitiveCommands = Arrays.asList(
            getCommand("register"), getCommand("login"), getCommand("changepassword"), getCommand("unregister"),
            getCommand("authme", "register"), getCommand("authme", "changepassword"),
            getCommand("email", "setpassword")
        );
        // Build array with entries like "/register ", "/authme cp ", "/authme changepass "
        String[] expectedEntries = sensitiveCommands.stream()
            .map(cmd -> buildCommandSyntaxes(cmd))
            .flatMap(List::stream)
            .map(syntax -> syntax + " ")
            .toArray(String[]::new);

        // when / then
        assertThat(LogFilterHelper.COMMANDS_TO_SKIP, containsInAnyOrder(expectedEntries));

    }

    private static CommandDescription getCommand(String label) {
        return findCommandWithLabel(label, ALL_COMMANDS);
    }

    private static CommandDescription getCommand(String parentLabel, String childLabel) {
        CommandDescription parent = getCommand(parentLabel);
        return findCommandWithLabel(childLabel, parent.getChildren());
    }

    private static CommandDescription findCommandWithLabel(String label, List<CommandDescription> commands) {
        return commands.stream()
            .filter(cmd -> cmd.getLabels().contains(label))
            .findFirst().orElseThrow(() -> new IllegalArgumentException(label));
    }

    /**
     * Returns all "command syntaxes" from which the given command can be reached.
     * For example, the result might be a List containing "/authme changepassword", "/authme changepass",
     * "/authme cp", "/authme:authme changepassword" etc.
     *
     * @param command the command to build syntaxes for
     * @return command syntaxes
     */
    private static List<String> buildCommandSyntaxes(CommandDescription command) {
        List<String> prefixes = getCommandPrefixes(command);

        return command.getLabels()
            .stream()
            .map(label -> Lists.transform(prefixes, p -> p + label))
            .flatMap(List::stream)
            .collect(Collectors.toList());
    }

    private static List<String> getCommandPrefixes(CommandDescription command) {
        if (command.getParent() == null) {
            return Arrays.asList("/", "/authme:");
        }
        return command.getParent().getLabels()
            .stream()
            .map(label -> new String[]{"/" + label + " ", "/authme:" + label + " "})
            .flatMap(Arrays::stream)
            .collect(Collectors.toList());
    }
}
