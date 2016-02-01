package fr.xephi.authme.command;

import static fr.xephi.authme.permission.DefaultPermission.OP_ONLY;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;

import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.WrapperMock;

/**
 * Test for {@link CommandInitializer} to guarantee the integrity of the defined commands.
 */
public class CommandInitializerTest {

    /**
     * Defines the maximum allowed depths for nesting CommandDescription instances.
     * Note that the depth starts at 0 (e.g. /authme), so a depth of 2 is something like /authme hello world
     */
    private static int MAX_ALLOWED_DEPTH = 1;

    private static Set<CommandDescription> commands;

    @BeforeClass
    public static void initializeCommandManager() {
        WrapperMock.createInstance();
        commands = CommandInitializer.buildCommands();
    }

    @Test
    public void shouldInitializeCommands() {
        // given/when/then
        // It obviously doesn't make sense to test much of the concrete data
        // that is being initialized; we just want to guarantee with this test
        // that data is indeed being initialized and we take a few "probes"
        assertThat(commands.size(), equalTo(9));
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
        walkThroughCommands(commands, descriptionTester);
    }

    /** Ensure that all children of a command stored the parent. */
    @Test
    public void shouldHaveConnectionBetweenParentAndChild() {
        // given
        BiConsumer connectionTester = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                if (!command.getChildren().isEmpty()) {
                    for (CommandDescription child : command.getChildren()) {
                        assertThat(command.equals(child.getParent()), equalTo(true));
                    }
                }
                // Checking that the parent has the current command as child is redundant as this is how we can traverse
                // the "command tree" in the first place - if we're here, it's that the parent definitely has the
                // command as child.
            }
        };

        // when/then
        walkThroughCommands(commands, connectionTester);
    }

    @Test
    public void shouldUseProperLowerCaseLabels() {
        // given
        final Pattern invalidPattern = Pattern.compile("\\s");
        BiConsumer labelFormatTester = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                for (String label : command.getLabels()) {
                    if (!label.equals(label.toLowerCase())) {
                        fail("Label '" + label + "' should be lowercase");
                    } else if (invalidPattern.matcher(label).matches()) {
                        fail("Label '" + label + "' has whitespace");
                    }
                }
            }
        };

        // when/then
        walkThroughCommands(commands, labelFormatTester);
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
                assertThat("All bindings are unique for command with bindings '" + newMappings + "'",
                    commandMappings.size() == initialSize + newMappings.size(), equalTo(true));
            }
        };

        // when/then
        walkThroughCommands(commands, uniqueMappingTester);
    }

    /**
     * The description should provide a very short description of the command and shouldn't end in a ".", whereas the
     * detailed description should be longer and end with a period.
     */
    @Test
    public void shouldHaveProperDescription() {
        // given
        BiConsumer descriptionTester = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                String forCommandText = " for command with labels '" + command.getLabels() + "'";

                assertThat("has description" + forCommandText,
                    StringUtils.isEmpty(command.getDescription()), equalTo(false));
                assertThat("short description doesn't end in '.'" + forCommandText,
                    command.getDescription().endsWith("."), equalTo(false));
                assertThat("has detailed description" + forCommandText,
                    StringUtils.isEmpty(command.getDetailedDescription()), equalTo(false));
                assertThat("detailed description ends in '.'" + forCommandText,
                    command.getDetailedDescription().endsWith("."), equalTo(true));
            }
        };

        // when/then
        walkThroughCommands(commands, descriptionTester);
    }

    /**
     * Check that the implementation of {@link ExecutableCommand} a command points to is the same for each type:
     * it is inefficient to instantiate the same type multiple times.
     */
    @Test
    public void shouldNotHaveMultipleInstancesOfSameExecutableCommandSubType() {
        // given
        final Map<Class<? extends ExecutableCommand>, ExecutableCommand> implementations = new HashMap<>();
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

        // when/then
        walkThroughCommands(commands, descriptionTester);
    }

    @Test
    public void shouldHaveOptionalArgumentsAfterMandatoryOnes() {
        // given
        BiConsumer argumentOrderTester = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                boolean encounteredOptionalArg = false;
                for (CommandArgumentDescription argument : command.getArguments()) {
                    if (argument.isOptional()) {
                        encounteredOptionalArg = true;
                    } else if (!argument.isOptional() && encounteredOptionalArg) {
                        fail("Mandatory arguments should come before optional ones for command with labels '"
                            + command.getLabels() + "'");
                    }
                }
            }
        };

        // when/then
        walkThroughCommands(commands, argumentOrderTester);
    }

    /**
     * Ensure that a command with children (i.e. a base command) doesn't define any arguments. This might otherwise
     * clash with the label of the child.
     */
    @Test
    public void shouldNotHaveArgumentsIfCommandHasChildren() {
        // given
        BiConsumer noArgumentForParentChecker = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                // Fail if the command has children and has arguments at the same time
                // Exception: If the parent only has one child defining the help label, it is acceptable
                if (!command.getChildren().isEmpty() && !command.getArguments().isEmpty()
                        && (command.getChildren().size() != 1 || !command.getChildren().get(0).hasLabel("help"))) {
                    fail("Parent command (labels='" + command.getLabels() + "') should not have any arguments");
                }
            }
        };

        // when/then
        walkThroughCommands(commands, noArgumentForParentChecker);
    }

    /**
     * Test that commands defined with the OP_ONLY default permission have at least one admin permission node.
     */
    @Test
    public void shouldNotHavePlayerPermissionIfDefaultsToOpOnly() {
        // given
        BiConsumer adminPermissionChecker = new BiConsumer() {
            // The only exception to this check is the force login command, which should default to OP_ONLY
            // but semantically it is a player permission
            final List<String> forceLoginLabels = Arrays.asList("forcelogin", "login");

            @Override
            public void accept(CommandDescription command, int depth) {
                CommandPermissions permissions = command.getCommandPermissions();
                if (permissions != null && OP_ONLY.equals(permissions.getDefaultPermission())) {
                    if (!hasAdminNode(permissions) && !command.getLabels().equals(forceLoginLabels)) {
                        fail("The command with labels " + command.getLabels() + " has OP_ONLY default "
                            + "permission but no permission node on admin level");
                    }
                }
            }

            private boolean hasAdminNode(CommandPermissions permissions) {
                for (PermissionNode node : permissions.getPermissionNodes()) {
                    if (node instanceof AdminPermission) {
                        return true;
                    }
                }
                return false;
            }
        };

        // when/then
        walkThroughCommands(commands, adminPermissionChecker);
    }

    /**
     * Tests that multiple CommandDescription instances pointing to the same ExecutableCommand use the same
     * count of arguments.
     */
    @Test
    public void shouldPointToSameExecutableCommandWithConsistentArgumentCount() {
        // given
        final Map<Class<? extends ExecutableCommand>, Integer> mandatoryArguments = new HashMap<>();
        final Map<Class<? extends ExecutableCommand>, Integer> totalArguments = new HashMap<>();

        BiConsumer argChecker = new BiConsumer() {
            @Override
            public void accept(CommandDescription command, int depth) {
                testCollectionForCommand(command, CommandUtils.getMinNumberOfArguments(command), mandatoryArguments);
                testCollectionForCommand(command, CommandUtils.getMaxNumberOfArguments(command), totalArguments);
            }
            private void testCollectionForCommand(CommandDescription command, int argCount,
                                                  Map<Class<? extends ExecutableCommand>, Integer> collection) {
                final Class<? extends ExecutableCommand> clazz = command.getExecutableCommand().getClass();
                Integer existingCount = collection.get(clazz);
                if (existingCount != null) {
                    String commandDescription = "Command with label '" + command.getLabels().get(0) + "' and parent '"
                        + (command.getParent() != null ? command.getLabels().get(0) : "null") + "' ";
                    assertThat(commandDescription + "should point to " + clazz + " with arguments consistent to others",
                        argCount, equalTo(existingCount));
                } else {
                    collection.put(clazz, argCount);
                }
            }
        };

        // when / then
        walkThroughCommands(commands, argChecker);
    }


    // ------------
    // Helper methods
    // ------------
    private static void walkThroughCommands(Collection<CommandDescription> commands, BiConsumer consumer) {
        walkThroughCommands(commands, consumer, 0);
    }

    private static void walkThroughCommands(Collection<CommandDescription> commands, BiConsumer consumer, int depth) {
        for (CommandDescription command : commands) {
            consumer.accept(command, depth);
            if (!command.getChildren().isEmpty()) {
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
     * Get the absolute binding that a command defines. Note: Assumes that only the passed command can have
     * multiple labels; only considering the first label for all of the command's parents.
     *
     * @param command The command to process
     *
     * @return List of all bindings that lead to the command
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
