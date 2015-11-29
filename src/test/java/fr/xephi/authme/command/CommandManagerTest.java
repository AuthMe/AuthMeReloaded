package fr.xephi.authme.command;

import fr.xephi.authme.util.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

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
    private static int MAX_ALLOWED_DEPTH = 2;

    @Test
    public void shouldInitializeCommands() {
        // given/when
        CommandManager manager = new CommandManager(true);
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
        CommandManager manager = new CommandManager(true);
        Consumer descriptionTester = new Consumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                assertThat(depth <= MAX_ALLOWED_DEPTH, equalTo(true));
            }
        };

        // when/then
        List<CommandDescription> commands = manager.getCommandDescriptions();
        walkThroughCommands(commands, descriptionTester);
    }

    @Test
    @Ignore
    public void shouldNotDefineSameLabelTwice() {
        // given
        CommandManager manager = new CommandManager(true);
        Set<String> commandMappings = new HashSet<>();
        Consumer uniqueMappingTester = new Consumer() {
            @Override
            public void accept(CommandDescription command, int depth) {

            }
        };

        // when
        // TODO finish this
    }

    /**
     * The description should provide a very short description of the command and shouldn't end in a ".", whereas the
     * detailed description should be longer and end with a period.
     */
    @Test
    public void shouldHaveDescription() {
        // given
        CommandManager manager = new CommandManager(true);
        Consumer descriptionTester = new Consumer() {
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

        // when
        List<CommandDescription> commands = manager.getCommandDescriptions();
        walkThroughCommands(commands, descriptionTester);
    }

    @Test
    public void shouldNotHaveMultipleInstancesOfSameExecutableCommandSubType() {
        // given
        final Map<Class<? extends ExecutableCommand>, ExecutableCommand> implementations = new HashMap<>();
        CommandManager manager = new CommandManager(true);
        Consumer descriptionTester = new Consumer() {
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
        walkThroughCommands(commands, descriptionTester);
    }


    // ------------
    // Helper methods
    // ------------
    private static void walkThroughCommands(List<CommandDescription> commands, Consumer consumer) {
        walkThroughCommands(commands, consumer, 0);
    }

    private static void walkThroughCommands(List<CommandDescription> commands, Consumer consumer, int depth) {
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

    private interface Consumer {
        void accept(CommandDescription command, int depth);
    }

}
