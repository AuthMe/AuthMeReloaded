package fr.xephi.authme.command;

import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.util.CollectionUtils;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.google.common.base.Objects.firstNonNull;

/**
 * Command description - defines which labels ("names") will lead to a command and points to the
 * {@link ExecutableCommand} implementation that executes the logic of the command.
 *
 * CommandDescription instances are built hierarchically and have one parent or {@code null} for base commands
 * (main commands such as /authme) and may have multiple children extending the mapping of the parent: e.g. if
 * /authme has a child whose label is "register", then "/authme register" is the command that the child defines.
 */
public class CommandDescription {

    /**
     * Defines the labels to execute the command. For example, if labels are "register" and "r" and the parent is
     * the command for "/authme", then both "/authme register" and "/authme r" will be handled by this command.
     */
    private List<String> labels;
    /**
     * Command description.
     */
    private String description;
    /**
     * Detailed description of the command.
     */
    private String detailedDescription;
    /**
     * The executable command instance.
     */
    private ExecutableCommand executableCommand;
    /**
     * The parent command.
     */
    private CommandDescription parent;
    /**
     * The child commands that extend this command.
     */
    private List<CommandDescription> children = new ArrayList<>();
    /**
     * The arguments the command takes.
     */
    private List<CommandArgumentDescription> arguments;
    /**
     * Defines the command permissions.
     */
    private CommandPermissions permissions;

    /**
     * Private constructor. Use {@link CommandDescription#builder()} to create instances of this class.
     * <p />
     * Note for developers: Instances should be created with {@link CommandDescription#createInstance} to be properly
     * registered in the command tree.
     */
    private CommandDescription() {
    }

    /**
     * Create an instance for internal use.
     *
     * @param labels              List of command labels.
     * @param description         Command description.
     * @param detailedDescription Detailed comment description.
     * @param executableCommand   The executable command, or null.
     * @param parent              Parent command.
     * @param arguments           Command arguments.
     * @param permissions         The permissions required to execute this command.
     *
     * @return The created instance
     * @see CommandDescription#builder()
     */
    private static CommandDescription createInstance(List<String> labels, String description,
                                                 String detailedDescription, ExecutableCommand executableCommand,
                                                 CommandDescription parent, List<CommandArgumentDescription> arguments,
                                                 CommandPermissions permissions) {
        CommandDescription instance = new CommandDescription();
        instance.labels = labels;
        instance.description = description;
        instance.detailedDescription = detailedDescription;
        instance.executableCommand = executableCommand;
        instance.parent = parent;
        instance.arguments = arguments;
        instance.permissions = permissions;

        if (parent != null) {
            parent.addChild(instance);
        }
        return instance;
    }

    private void addChild(CommandDescription command) {
        children.add(command);
    }

    /**
     * Get all relative command labels.
     *
     * @return All relative labels labels.
     */
    public List<String> getLabels() {
        return this.labels;
    }

    /**
     * Check whether this command description has a specific command.
     *
     * @param commandLabel Command to check for.
     *
     * @return True if this command label equals to the param command.
     */
    public boolean hasLabel(String commandLabel) {
        for (String label : labels) {
            if (label.equalsIgnoreCase(commandLabel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the executable command.
     *
     * @return The executable command.
     */
    public ExecutableCommand getExecutableCommand() {
        return this.executableCommand;
    }

    /**
     * Get the parent command if this command description has a parent.
     *
     * @return Parent command, or null
     */
    public CommandDescription getParent() {
        return this.parent;
    }

    /**
     * Get all command children.
     *
     * @return Command children.
     */
    public List<CommandDescription> getChildren() {
        return this.children;
    }


    /**
     * Get all command arguments.
     *
     * @return Command arguments.
     */
    public List<CommandArgumentDescription> getArguments() {
        return this.arguments;
    }

    /**
     * Check whether this command has any arguments.
     *
     * @return True if this command has any arguments.
     */
    public boolean hasArguments() {
        return !getArguments().isEmpty();
    }

    /**
     * Get the command description.
     *
     * @return Command description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Get the command detailed description.
     *
     * @return Command detailed description.
     */
    public String getDetailedDescription() {
        return detailedDescription;
    }


    /**
     * Get the command permissions. Return null if the command doesn't require any permission.
     *
     * @return The command permissions.
     */
    public CommandPermissions getCommandPermissions() {
        return this.permissions;
    }

    public static CommandBuilder builder() {
        return new CommandBuilder();
    }

    /**
     * Builder for initializing CommandDescription objects.
     */
    public static final class CommandBuilder {
        private List<String> labels;
        private String description;
        private String detailedDescription;
        private ExecutableCommand executableCommand;
        private CommandDescription parent;
        private List<CommandArgumentDescription> arguments = new ArrayList<>();
        private CommandPermissions permissions;

        /**
         * Build a CommandDescription from the builder or throw an exception if mandatory
         * fields have not been set.
         *
         * @return The generated CommandDescription object
         */
        // TODO ljacqu 20151206 Move validation to the create instance method
        public CommandDescription build() {
            return createInstance(
                getOrThrow(labels, "labels"),
                firstNonNull(description, ""),
                firstNonNull(detailedDescription, ""),
                getOrThrow(executableCommand, "executableCommand"),
                firstNonNull(parent, null),
                arguments,
                permissions
            );
        }

        public CommandBuilder labels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        public CommandBuilder labels(String... labels) {
            return labels(asMutableList(labels));
        }

        public CommandBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CommandBuilder detailedDescription(String detailedDescription) {
            this.detailedDescription = detailedDescription;
            return this;
        }

        public CommandBuilder executableCommand(ExecutableCommand executableCommand) {
            this.executableCommand = executableCommand;
            return this;
        }

        public CommandBuilder parent(CommandDescription parent) {
            this.parent = parent;
            return this;
        }

        /**
         * Add an argument that the command description requires. This method can be called multiples times to add
         * multiple arguments.
         *
         * @param label The label of the argument (single word name of the argument)
         * @param description The description of the argument
         * @param isOptional True if the argument is option, false if it is mandatory
         *
         * @return The builder
         */
        public CommandBuilder withArgument(String label, String description, boolean isOptional) {
            arguments.add(new CommandArgumentDescription(label, description, isOptional));
            return this;
        }

        public CommandBuilder permissions(DefaultPermission defaultPermission,
                                          PermissionNode... permissionNodes) {
            this.permissions = new CommandPermissions(asMutableList(permissionNodes), defaultPermission);
            return this;
        }

        @SafeVarargs
        private static <T> List<T> asMutableList(T... items) {
            return new ArrayList<>(Arrays.asList(items));
        }

        private static <T> T getOrThrow(T element, String elementName) {
            if (!isEmpty(element)) {
                return element;
            }
            throw new RuntimeException("The element '" + elementName + "' may not be empty in CommandDescription");
        }

        private static <T> boolean isEmpty(T element) {
            if (element == null) {
                return true;
            } else if (element instanceof Collection<?>) {
                return ((Collection<?>) element).isEmpty();
            } else if (element instanceof String) {
                return StringUtils.isEmpty((String) element);
            }
            return false;
        }

    }

}
