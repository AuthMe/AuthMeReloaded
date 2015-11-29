package fr.xephi.authme.command;

import fr.xephi.authme.util.StringUtils;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link CommandManager}.
 */
public class CommandManagerTest {

    /**
     * Defines the maximum allowed depths for nesting CommandDescription instances.
     * Note that the depth starts at 0 (e.g. /authme), so a depth of 2 is something like /authme hello world
     */
    private static int MAX_ALLOWED_DEPTH = 1;

    private static CommandManager manager;

    @BeforeClass
    public static void initializeCommandManager() {
        manager = new CommandManager(true);
    }

    @Test
    public void shouldInitializeCommands() {
        // given/when
        int commandCount = manager.getCommandDescriptionCount();
        List<CommandDescription> commands = manager.getCommandDescriptions();

        // then
        // It obviously doesn't make sense to test much of the concrete data
        // that is being initialized; we just want to guarantee with this test
        // that data is indeed being initialized and we take a few "probes"
        assertThat(commandCount, equalTo(9));
        assertThat(commandsIncludeLabel(commands, "authme"), equalTo(true));
        assertThat(commandsIncludeLabel(commands, "register"), equalTo(true));
        assertThat(commandsIncludeLabel(commands, "help"), equalTo(false));
    }

    @Test
    public void shouldNotBeNestedExcessively() {
        // given
        BiConsumer descriptionTester = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                assertThat(depth <= MAX_ALLOWED_DEPTH, equalTo(true));
            }
        };

        // when/then
        walkThroughCommands(manager.getCommandDescriptions(), descriptionTester);
    }

    @Test
    public void shouldNotDefineSameLabelTwice() {
        // given
        final Set<String> commandMappings = new HashSet<>();
        BiConsumer uniqueMappingTester = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                int initialSize = commandMappings.size();
                List<String> newMappings = getAbsoluteLabels(command);
                commandMappings.addAll(newMappings);
                // Set only contains unique entries, so we just check after adding all new mappings that the size
                // of the Set corresponds to our expectation
                assertThat("All bindings are unique for command with bindings '" + command.getLabels() + "'",
                    commandMappings.size() == initialSize + newMappings.size(), equalTo(true));
            }
        };

        // when/then
        walkThroughCommands(manager.getCommandDescriptions(), uniqueMappingTester);
    }

    /**
     * The description should provide a very short description of the command and shouldn't end in a ".", whereas the
     * detailed description should be longer and end with a period.
     */
    @Test
    public void shouldHaveDescription() {
        // given
        BiConsumer descriptionTester = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                assertThat("has description", StringUtils.isEmpty(command.getDescription()), equalTo(false));
                assertThat("short description doesn't end in '.'", command.getDescription().endsWith("."),
                    equalTo(false));
                assertThat("has detailed description", StringUtils.isEmpty(command.getDetailedDescription()),
                    equalTo(false));
                assertThat("detailed description ends in '.'", command.getDetailedDescription().endsWith("."),
                    equalTo(true));
            }
        };

        // when/then
        walkThroughCommands(manager.getCommandDescriptions(), descriptionTester);
    }

    /**
     * Check that the implementation of {@link ExecutableCommand} a command points to is the same for each type:
     * it is inefficient to instantiate the same type multiple times.
     */
    @Test
    public void shouldNotHaveMultipleInstancesOfSameExecutableCommandSubType() {
        // given
        final Map<Class<? extends ExecutableCommand>, ExecutableCommand> implementations = new HashMap<>();
        CommandManager manager = new CommandManager(true);
        BiConsumer descriptionTester = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                assertThat(command.getExecutableCommand(), not(nullValue()));
                ExecutableCommand commandExec = command.getExecutableCommand();
                ExecutableCommand storedExec = implementations.get(command.getExecutableCommand().getClass());
                if (storedExec != null) {
                    assertThat("has same implementation of '" + storedExec.getClass().getName() + "' for command with "
                        + "parent " + (command.getParent() == null ? "null" : command.getParent().getLabels()),
                        storedExec == commandExec, equalTo(true));
                } else {
                    implementations.put(commandExec.getClass(), commandExec);
                }
            }
        };

        // when
        List<CommandDescription> commands = manager.getCommandDescriptions();

        // then
        walkThroughCommands(commands, descriptionTester);
    }


    // ------------
    // Helper methods
    // ------------
    private static void walkThroughCommands(List<CommandDescription> commands, BiConsumer consumer) {
        walkThroughCommands(commands, consumer, 0);
    }

    private static void walkThroughCommands(List<CommandDescription> commands, BiConsumer consumer, int depth) {
        for (CommandDescription command : commands) {
            consumer.accept(command, depth);
            if (command.hasChildren()) {
                walkThroughCommands(command.getChildren(), consumer, depth + 1);
            }
        }
    }

    private static boolean commandsIncludeLabel(Iterable<CommandDescription> commands, String label) {
        for (CommandDescription command : commands) {
            if (command.getLabels().contains(label)) {
                return true;
            }
        }
        return false;
    }

    private interface BiConsumer {
        void accept(CommandDescription command, int depth);
    }

    /**
     * Get the absolute label that a command defines. Note: Assumes that only the passed command might have
     * multiple labels; only considering the first label for all of the command's parents.
     *
     * @param command The command to verify
     *
     * @return The full command binding
     */
    private static List<String> getAbsoluteLabels(CommandDescription command) {
        String parentPath = "";
        CommandDescription elem = command.getParent();
        while (elem != null) {
            parentPath = elem.getLabels().get(0) + " " + parentPath;
            elem = elem.getParent();
        }
        parentPath = parentPath.trim();

        List<String> bindings = new ArrayList<>(command.getLabels().size());
        for (String label : command.getLabels()) {
            bindings.add(StringUtils.join(" ", parentPath, label));
        }
        return bindings;
    }

}
