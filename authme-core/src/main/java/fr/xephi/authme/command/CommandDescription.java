package fr.xephi.authme.command;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Arrays.asList;

/**
 * Command description – defines which labels ("names") will lead to a command and points to the
 * {@link ExecutableCommand} implementation that executes the logic of the command.
 * <p>
 * CommandDescription instances are built hierarchically: they have one parent, or {@code null} for base commands
 * (main commands such as {@code /authme}), and may have multiple children extending the mapping of the parent: e.g. if
 * {@code /authme} has a child whose label is {@code "register"}, then {@code /authme register} is the command that
 * the child defines.
 */
@SuppressWarnings("checkstyle:FinalClass") // Justification: class is mocked in multiple tests
public class CommandDescription {

    /**
     * Defines the labels to execute the command. For example, if labels are "register" and "r" and the parent is
     * the command for "/authme", then both "/authme register" and "/authme r" will be handled by this command.
     */
    private List<String> labels;
    /**
     * Short description of the command.
     */
    private String description;
    /**
     * Detailed description of what the command does.
     */
    private String detailedDescription;
    /**
     * The class implementing the command described by this object.
     */
    private Class<? extends ExecutableCommand> executableCommand;
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
     * Permission node required to execute this command.
     */
    private PermissionNode permission;

    /**
     * Private constructor.
     * <p>
     * Note for developers: Instances should be created with {@link CommandBuilder#register()} to be properly
     * registered in the command tree.
     *
     * @param labels              command labels
     * @param description         description of the command
     * @param detailedDescription detailed command description
     * @param executableCommand   class of the command implementation
     * @param parent              parent command
     * @param arguments           command arguments
     * @param permission          permission node required to execute this command
     */
    private CommandDescription(List<String> labels, String description, String detailedDescription, 
                               Class<? extends ExecutableCommand> executableCommand, CommandDescription parent,
                               List<CommandArgumentDescription> arguments,  PermissionNode permission) {
        this.labels = labels;
        this.description = description;
        this.detailedDescription = detailedDescription;
        this.executableCommand = executableCommand;
        this.parent = parent;
        this.arguments = arguments;
        this.permission = permission;
    }

    /**
     * Return all relative labels of this command. For example, if this object describes {@code /authme register} and
     * {@code /authme r}, then it will return a list with {@code register} and {@code r}. The parent label
     * {@code authme} is not returned.
     *
     * @return All labels of the command description.
     */
    public List<String> getLabels() {
        return labels;
    }

    /**
     * Check whether this command description has the given label.
     *
     * @param commandLabel The label to check for.
     *
     * @return {@code true} if this command contains the given label, {@code false} otherwise.
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
     * Return the {@link ExecutableCommand} class implementing this command.
     *
     * @return The executable command class
     */
    public Class<? extends ExecutableCommand> getExecutableCommand() {
        return executableCommand;
    }

    /**
     * Return the parent.
     *
     * @return The parent command, or null for base commands
     */
    public CommandDescription getParent() {
        return parent;
    }

    /**
     * Return the number of labels necessary to get to this command. This corresponds to the number of parents + 1.
     *
     * @return The number of labels, e.g. for "/authme abc def" the label count is 3
     */
    public int getLabelCount() {
        if (parent == null) {
            return 1;
        }
        return parent.getLabelCount() + 1;
    }

    /**
     * Return all command children.
     *
     * @return Command children.
     */
    public List<CommandDescription> getChildren() {
        return children;
    }

    /**
     * Return all arguments the command takes.
     *
     * @return Command arguments.
     */
    public List<CommandArgumentDescription> getArguments() {
        return arguments;
    }

    /**
     * Return a short description of the command.
     *
     * @return Command description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Return a detailed description of the command.
     *
     * @return Detailed description.
     */
    public String getDetailedDescription() {
        return detailedDescription;
    }

    /**
     * Return the permission node required to execute the command.
     *
     * @return The permission node, or null if none are required to execute the command.
     */
    public PermissionNode getPermission() {
        return permission;
    }

    /**
     * Return a builder instance to create a new command description.
     *
     * @return The builder
     */
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
        private Class<? extends ExecutableCommand> executableCommand;
        private CommandDescription parent;
        private List<CommandArgumentDescription> arguments = new ArrayList<>();
        private PermissionNode permission;

        /**
         * Build a CommandDescription and register it onto the parent if available.
         *
         * @return The generated CommandDescription object
         */
        public CommandDescription register() {
            CommandDescription command = build();

            if (command.parent != null) {
                command.parent.children.add(command);
            }
            return command;
        }

        /**
         * Build a CommandDescription (without registering it on the parent).
         *
         * @return The generated CommandDescription object
         */
        public CommandDescription build() {
            checkArgument(!Utils.isCollectionEmpty(labels), "Labels may not be empty");
            checkArgument(!StringUtils.isBlank(description), "Description may not be empty");
            checkArgument(!StringUtils.isBlank(detailedDescription), "Detailed description may not be empty");
            checkArgument(executableCommand != null, "Executable command must be set");
            // parents and permissions may be null; arguments may be empty

            return new CommandDescription(labels, description, detailedDescription, executableCommand,
                                          parent, arguments, permission);
        }

        public CommandBuilder labels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        public CommandBuilder labels(String... labels) {
            return labels(asList(labels));
        }

        public CommandBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CommandBuilder detailedDescription(String detailedDescription) {
            this.detailedDescription = detailedDescription;
            return this;
        }

        public CommandBuilder executableCommand(Class<? extends ExecutableCommand> executableCommand) {
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
         * @param isOptional True if the argument is optional, false if it is mandatory
         *
         * @return The builder
         */
        public CommandBuilder withArgument(String label, String description, boolean isOptional) {
            arguments.add(new CommandArgumentDescription(label, description, isOptional));
            return this;
        }

        /**
         * Add a permission node that a user must have to execute the command.
         *
         * @param permission The PermissionNode to add
         * @return The builder
         */
        public CommandBuilder permission(PermissionNode permission) {
            this.permission = permission;
            return this;
        }
    }

}
