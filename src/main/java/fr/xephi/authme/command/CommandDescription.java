package fr.xephi.authme.command;

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
     * Constructor.
     *
     * @param executableCommand   The executable command, or null.
     * @param labels              List of command labels.
     * @param description         Command description.
     * @param detailedDescription Detailed comment description.
     * @param parent              Parent command.
     */
    @Deprecated
    public CommandDescription(ExecutableCommand executableCommand, List<String> labels, String description, String detailedDescription, CommandDescription parent) {
        setExecutableCommand(executableCommand);
        this.labels = labels;
        this.description = description;
        this.detailedDescription = detailedDescription;
        setParent(parent);
        this.arguments = new ArrayList<>();
    }

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

    /**
     * Get the label most similar to the reference. The first label will be returned if no reference was supplied.
     *
     * @param reference The command reference.
     *
     * @return The most similar label, or the first label. An empty label will be returned if no label was set.
     */
    public String getLabel(CommandParts reference) {
        // Ensure there's any item in the command list
        if (this.labels.size() == 0)
            return "";

        // Return the first label if we can't use the reference
        if (reference == null)
            return this.labels.get(0);

        // Get the correct label from the reference
        String preferred = reference.get(getParentCount());

        // Check whether the preferred label is in the label list
        double currentDifference = -1;
        String currentLabel = this.labels.get(0);
        for (String entry : this.labels) {
            double entryDifference = StringUtils.getDifference(entry, preferred);
            if (entryDifference < currentDifference || currentDifference < 0) {
                currentDifference = entryDifference;
                currentLabel = entry;
            }
        }

        // Return the most similar label
        return currentLabel;
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
        for (String label : this.labels) {
            if (label.equalsIgnoreCase(commandLabel)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check whether this command label is applicable with a command reference. This doesn't check if the parent
     * are suitable too.
     *
     * @param commandReference The command reference.
     *
     * @return True if the command reference is suitable to this command label, false otherwise.
     */
    public boolean isSuitableLabel(CommandParts commandReference) {
        // Get the parent count
        //getParent() = getParent().getParentCount() + 1
        String element = commandReference.get(getParentCount());

        // Check whether this command description has this command label
        for (String label : labels) {
            if (label.equalsIgnoreCase(element)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the command reference.
     *
     * @param reference The reference to use as template, which is used to choose the most similar reference.
     *
     * @return Command reference.
     */
    public CommandParts getCommandReference(CommandParts reference) {
        // Build the reference
        List<String> referenceList = new ArrayList<>();

        // Check whether this command has a parent, if so, add the absolute parent command
        if (getParent() != null) {
            referenceList.addAll(getParent().getCommandReference(reference).getList());
        }

        // Get the current label
        referenceList.add(getLabel(reference));

        // Return the reference
        return new CommandParts(referenceList);
    }

    /**
     * Get the difference between this command and another command reference.
     *
     * @param other The other command reference.
     *
     * @return The command difference. Zero if there's no difference. A negative number on error.
     */
    public double getCommandDifference(CommandParts other) {
        return getCommandDifference(other, false);
    }

    /**
     * Get the difference between this command and another command reference.
     *
     * @param other       The other command reference.
     * @param fullCompare True to fully compare both command references.
     *
     * @return The command difference. Zero if there's no difference. A negative number on error.
     */
    public double getCommandDifference(CommandParts other, boolean fullCompare) {
        // Make sure the reference is valid
        if (other == null)
            return -1;

        // Get the command reference
        CommandParts reference = getCommandReference(other);

        // Compare the two references, return the result
        return CommandUtils.getDifference(reference.getList(),
            CollectionUtils.getRange(other.getList(), 0, reference.getList().size()), fullCompare);
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
     * Set the executable command.
     *
     * @param executableCommand The executable command.
     */
    public void setExecutableCommand(ExecutableCommand executableCommand) {
        this.executableCommand = executableCommand;
    }

    /**
     * Execute the command, if possible.
     *
     * @param sender           The command sender that triggered the execution of this command.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True on success, false on failure.
     */
    public boolean execute(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Execute the command, return the result
        return getExecutableCommand().executeCommand(sender, commandReference, commandArguments);
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
     * Get the number of parent this description has.
     *
     * @return The number of parents.
     */
    public int getParentCount() {
        // Check whether the this description has a parent
        if (!hasParent())
            return 0;

        // Get the parent count of the parent, return the result
        return getParent().getParentCount() + 1;
    }

    /**
     * Set the parent command.
     *
     * @param parent Parent command.
     *
     * @return True on success, false on failure.
     */
    public boolean setParent(CommandDescription parent) {
        // Make sure the parent is different
        if (this.parent == parent)
            return true;

        // Set the parent
        this.parent = parent;

        // Make sure the parent isn't null
        if (parent == null)
            return true;

        // Add this description as a child to the parent
        return parent.addChild(this);
    }

    /**
     * Check whether the plugin description has a parent command.
     *
     * @return True if the description has a parent command, false otherwise.
     */
    public boolean hasParent() {
        return this.parent != null;
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
     * Add a child to the command description.
     *
     * @param commandDescription The child to add.
     *
     * @return True on success, false on failure.
     */
    public boolean addChild(CommandDescription commandDescription) {
        // Make sure the description is valid
        if (commandDescription == null)
            return false;

        // Make sure the child doesn't exist already
        if (isChild(commandDescription))
            return true;

        // The command description to add as a child
        if (!this.children.add(commandDescription))
            return false;

        // Set this description as parent on the child
        return commandDescription.setParent(this);
    }

    /**
     * Check whether this command has any child labels.
     *
     * @return True if this command has any child labels.
     */
    public boolean hasChildren() {
        return (this.children.size() != 0);
    }

    /**
     * Check if this command description has a specific child.
     *
     * @param commandDescription The command description to check for.
     *
     * @return True if this command description has the specific child, false otherwise.
     */
    public boolean isChild(CommandDescription commandDescription) {
        // Make sure the description is valid
        if (commandDescription == null)
            return false;

        // Check whether this child exists, return the result
        return this.children.contains(commandDescription);
    }

    /**
     * Add an argument.
     *
     * @param argument The argument to add.
     *
     * @return True if succeed, false if failed.
     */
    public boolean addArgument(CommandArgumentDescription argument) {
        // Make sure the argument is valid
        if (argument == null)
            return false;

        // Add the argument, return the result
        return this.arguments.add(argument);
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
     * Find the best suitable command for a query reference.
     *
     * @param queryReference The query reference to find a command for.
     *
     * @return The command found, or null.
     */
    public FoundCommandResult findCommand(final CommandParts queryReference) {
        // Make sure the command reference is valid
        List<String> queryRef = queryReference.getList();
        if (queryRef.isEmpty()) {
            return null;
        }

        // Check whether this description is for the last element in the command reference, if so return the current command
        if (queryRef.size() <= getParentCount() + 1) {
            return new FoundCommandResult(
                this,
                getCommandReference(queryReference),
                new CommandParts(new ArrayList<String>()),
                queryReference);
        }

        // Get the new command reference and arguments
        CommandParts newReference = new CommandParts(CollectionUtils.getRange(queryReference.getList(), 0, getParentCount() + 1));
        CommandParts newArguments = new CommandParts(CollectionUtils.getRange(queryReference.getList(), getParentCount() + 1));

        // Handle the child's, if this command has any
        if (getChildren().size() > 0) {
            // Get a new instance of the child's list, and sort them by their difference in comparison to the query reference
            List<CommandDescription> commandChildren = new ArrayList<>(getChildren());
            Collections.sort(commandChildren, new Comparator<CommandDescription>() {
                @Override
                public int compare(CommandDescription o1, CommandDescription o2) {
                    return Double.compare(
                        o1.getCommandDifference(queryReference),
                        o2.getCommandDifference(queryReference));
                }
            });

            // Get the difference of the first child in the list
            double firstChildDifference = commandChildren.get(0).getCommandDifference(queryReference, true);

            // Check if the reference perfectly suits the arguments of the current command if it doesn't perfectly suits a child command
            if (firstChildDifference > 0.0)
                if (getSuitableArgumentsDifference(queryReference) == 0)
                    return new FoundCommandResult(this, newReference, newArguments, queryReference);

            // Loop through each child
            for (CommandDescription child : commandChildren) {
                // Get the best suitable command
                FoundCommandResult result = child.findCommand(queryReference);
                if (result != null)
                    return result;
            }
        }

        // Check if the remaining command reference elements fit the arguments for this command
        if (getSuitableArgumentsDifference(queryReference) >= 0)
            return new FoundCommandResult(this, newReference, newArguments, queryReference);

        // No command found, return null
        return null;
    }

    /**
     * Check if the remaining command reference elements are suitable with arguments of the current command description,
     * and get the difference in argument count.
     *
     * @param commandReference The command reference.
     *
     * @return The difference in argument count between the reference and the actual command.
     */
    public int getSuitableArgumentsDifference(CommandParts commandReference) {
        // Make sure the command reference is valid
        List<String> labels = commandReference.getList();
        if (labels.isEmpty()) {
            return -1;
        }

        // Get the remaining command reference element count
        int remainingElementCount = labels.size() - getParentCount() - 1;

        // Check if there are too few arguments
        int minArguments = CommandUtils.getMinNumberOfArguments(this);
        if (minArguments > remainingElementCount) {
            return Math.abs(minArguments - remainingElementCount);
        }

        // Check if there are too many arguments
        int maxArguments = CommandUtils.getMaxNumberOfArguments(this);
        if (maxArguments >= 0 && maxArguments < remainingElementCount) {
            return Math.abs(remainingElementCount - maxArguments);
        }

        // The argument count is the same
        return 0;
    }

    /**
     * Get the command permissions. Return null if the command doesn't require any permission.
     *
     * @return The command permissions.
     */
    public CommandPermissions getCommandPermissions() {
        return this.permissions;
    }

    /**
     * Set the command permissions.
     *
     * @param permissionNode    The permission node required.
     * @param defaultPermission The default permission.
     */
    public void setCommandPermissions(PermissionNode permissionNode, CommandPermissions.DefaultPermission defaultPermission) {
        this.permissions = new CommandPermissions(permissionNode, defaultPermission);
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
        public CommandDescription build() {
            return createInstance(
                getOrThrow(labels, "labels"),
                firstNonNull(description, ""),
                firstNonNull(detailedDescription, ""),
                getOrThrow(executableCommand, "executableCommand"),
                firstNonNull(parent, null),
                arguments,
                firstNonNull(permissions, null)
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

        public CommandBuilder permissions(CommandPermissions.DefaultPermission defaultPermission,
                                          PermissionNode... permissionNodes) {
            this.permissions = new CommandPermissions(asMutableList(permissionNodes), defaultPermission);
            return this;
        }

        @SafeVarargs
        private static <T> List<T> asMutableList(T... items) {
            return new ArrayList<>(Arrays.asList(items));
        }

        private static <T> T firstNonNull(T first, T second) {
            return first != null ? first : second;
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
