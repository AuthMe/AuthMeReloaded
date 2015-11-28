package fr.xephi.authme.command;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.command.CommandSender;

import java.util.*;

/**
 */
public class CommandDescription {

    /**
     * Defines the acceptable labels.
     */
    private List<String> labels;
    /**
     * Command description.
     */
    private String description;
    /**
     * Detailed description.
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
     * The child labels.
     */
    // TODO: Remove list instantiation once Builder is the only way to construct objects
    private List<CommandDescription> children = new ArrayList<>();
    /**
     * The command arguments.
     */
    private List<CommandArgumentDescription> arguments;
    /**
     * Defines whether there is an argument maximum or not.
     */
    private boolean noArgumentMaximum;
    /**
     * Defines the command permissions.
     */
    private CommandPermissions permissions;

    /**
     * Constructor.
     *
     * @param executableCommand   The executable command, or null.
     * @param label               Command label.
     * @param description         Command description.
     * @param detailedDescription Detailed comment description.
     * @param parent              Parent command.
     */
    public CommandDescription(ExecutableCommand executableCommand, String label, String description, String detailedDescription, CommandDescription parent) {
        this(executableCommand, label, description, parent, detailedDescription, null);
    }

    /**
     * Constructor.
     *
     * @param executableCommand   The executable command, or null.
     * @param labels              List of command labels.
     * @param description         Command description.
     * @param detailedDescription Detailed comment description.
     * @param parent              Parent command.
     */
    public CommandDescription(ExecutableCommand executableCommand, List<String> labels, String description, String detailedDescription, CommandDescription parent) {
        this(executableCommand, labels, description, detailedDescription, parent, null);
    }

    /**
     * Constructor.
     *
     * @param executableCommand   The executable command, or null.
     * @param label               Command label.
     * @param description         Command description.
     * @param parent              Parent command.
     * @param detailedDescription Detailed comment description.
     * @param arguments           Command arguments.
     */
    public CommandDescription(ExecutableCommand executableCommand, String label, String description, CommandDescription parent, String detailedDescription, List<CommandArgumentDescription> arguments) {
        setExecutableCommand(executableCommand);
        this.labels = Collections.singletonList(label);
        this.description = description;
        this.detailedDescription = detailedDescription;
        setParent(parent);
        setArguments(arguments);
    }

    /**
     * Constructor.
     *
     * @param executableCommand   The executable command, or null.
     * @param labels              List of command labels.
     * @param description         Command description.
     * @param detailedDescription Detailed comment description.
     * @param parent              Parent command.
     * @param arguments           Command arguments.
     */
    public CommandDescription(ExecutableCommand executableCommand, List<String> labels, String description, String detailedDescription, CommandDescription parent, List<CommandArgumentDescription> arguments) {
        setExecutableCommand(executableCommand);
        this.labels = labels;
        this.description = description;
        this.detailedDescription = detailedDescription;
        setParent(parent);
        setArguments(arguments);
    }

    /**
     * Private constructor. Use {@link CommandDescription#builder()} to create instances of this class.
     *
     * @param executableCommand   The executable command, or null.
     * @param labels              List of command labels.
     * @param description         Command description.
     * @param detailedDescription Detailed comment description.
     * @param parent              Parent command.
     * @param arguments           Command arguments.
     */
    private CommandDescription(List<String> labels, String description, String detailedDescription,
                               ExecutableCommand executableCommand, CommandDescription parent,
                               List<CommandDescription> children, List<CommandArgumentDescription> arguments,
                               boolean noArgumentMaximum, CommandPermissions permissions) {
        this.labels = labels;
        this.description = description;
        this.detailedDescription = detailedDescription;
        this.executableCommand = executableCommand;
        this.parent = parent;
        this.children = children;
        this.arguments = arguments;
        this.noArgumentMaximum = noArgumentMaximum;
        this.permissions = permissions;
    }

    /**
     * Check whether two labels are equal to each other.
     *
     * @param commandLabel      The first command label.
     * @param otherCommandLabel The other command label.
     *
     * @return True if the labels are equal to each other.
     */
    private static boolean commandLabelEquals(String commandLabel, String otherCommandLabel) {
        // Trim the command labels from unwanted whitespaces
        commandLabel = commandLabel.trim();
        otherCommandLabel = otherCommandLabel.trim();

        // Check whether the the two command labels are equal (case insensitive)
        return (commandLabel.equalsIgnoreCase(otherCommandLabel));
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
        // Check whether any command matches with the argument
        for (String entry : this.labels)
            if (commandLabelEquals(entry, commandLabel))
                return true;

        // No match found, return false
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
        // Make sure the command reference is valid
        if (commandReference.getCount() <= 0)
            return false;

        // Get the parent count
        String element = commandReference.get(getParentCount());

        // Check whether this command description has this command label
        return hasLabel(element);
    }

    /**
     * Get the absolute command label, without a slash.
     *
     * @return the absolute label
     */
    public String getAbsoluteLabel() {
        return getAbsoluteLabel(false);
    }

    /**
     * Get the absolute command label.
     *
     * @param includeSlash boolean
     *
     * @return Absolute command label.
     */
    public String getAbsoluteLabel(boolean includeSlash) {
        return getAbsoluteLabel(includeSlash, null);
    }

    /**
     * Get the absolute command label.
     *
     * @param includeSlash
     * @param reference
     *
     * @return Absolute command label.
     */
    public String getAbsoluteLabel(boolean includeSlash, CommandParts reference) {
        // Get the command reference, and make sure it is valid
        CommandParts out = getCommandReference(reference);
        if (out == null)
            return "";

        // Return the result
        return (includeSlash ? "/" : "") + out.toString();
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
        if (getParent() != null)
            referenceList.addAll(getParent().getCommandReference(reference).getList());

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
        return reference.getDifference(new CommandParts(other.getRange(0, reference.getCount())), fullCompare);
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
     * Check whether this command is executable, based on the assigned executable command.
     *
     * @return True if this command is executable.
     */
    public boolean isExecutable() {
        return this.executableCommand != null;
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
        // Make sure the command is executable
        if (!isExecutable())
            return false;

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
     * Set the children of this command.
     *
     * @param children New command children. Null to remove all children.
     */
    public void setChildren(List<CommandDescription> children) {
        // Check whether the children list should be cleared
        if (children == null)
            this.children.clear();

        else
            this.children = children;
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
        if (commandDescription == null) // TODO: After builder, commandDescription == null -> never
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

        // Make sure the argument isn't added already
        if (hasArgument(argument))
            return true;

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
     * Set the arguments of this command.
     *
     * @param arguments New command arguments. Null to clear the list of arguments.
     */
    public void setArguments(List<CommandArgumentDescription> arguments) {
        // Convert null into an empty argument list
        if (arguments == null) {
            // Note ljacqu 20151128: Temporary workaround to avoid null pointer exception. Soon we won't need setters
            // on the main class (-> complete instantiation via Builder)
            // TODO Remove this method once unused
            this.arguments = new ArrayList<>();
        } else {
            this.arguments = arguments;
        }
    }

    /**
     * Check whether an argument exists.
     *
     * @param argument The argument to check for.
     *
     * @return True if this argument already exists, false otherwise.
     */
    public boolean hasArgument(CommandArgumentDescription argument) {
        return argument != null && arguments.contains(argument);
    }

    /**
     * Check whether this command has any arguments.
     *
     * @return True if this command has any arguments.
     */
    public boolean hasArguments() {
        return !arguments.isEmpty();
    }

    /**
     * The minimum number of arguments required for this command.
     *
     * @return The minimum number of required arguments.
     */
    public int getMinimumArguments() {
        // Get the number of required and optional arguments
        int requiredArguments = 0;
        int optionalArgument = 0;

        // Loop through each argument
        for (CommandArgumentDescription argument : this.arguments) {
            // Check whether the command is optional
            if (!argument.isOptional()) {
                requiredArguments += optionalArgument + 1;
                optionalArgument = 0;

            } else
                optionalArgument++;
        }

        // Return the number of required arguments
        return requiredArguments;
    }

    /**
     * Get the maximum number of arguments.
     *
     * @return The maximum number of arguments. A negative number will be returned if there's no maximum.
     */
    public int getMaximumArguments() {
        // Check whether there is a maximum set
        if (this.noArgumentMaximum)
            // TODO ljacqu 20151128: Magic number
            return -1;

        // Return the maximum based on the registered arguments
        return this.arguments.size();
    }

    /**
     * Get the command description.
     *
     * @return Command description.
     */
    public String getDescription() {
        return hasDescription() ? this.description : this.detailedDescription;
    }

    /**
     * Check whether this command has any description.
     *
     * @return True if this command has any description.
     */
    public boolean hasDescription() {
        return !StringUtils.isEmpty(description);
    }

    /**
     * Get the command detailed description.
     *
     * @return Command detailed description.
     */
    public String getDetailedDescription() {
        return StringUtils.isEmpty(detailedDescription) ? this.detailedDescription : this.description;
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
        if (queryReference.getCount() <= 0)
            return null;

        // Check whether this description is for the last element in the command reference, if so return the current command
        if (queryReference.getCount() <= getParentCount() + 1)
            return new FoundCommandResult(
                this,
                getCommandReference(queryReference),
                new CommandParts(),
                queryReference);

        // Get the new command reference and arguments
        CommandParts newReference = new CommandParts(queryReference.getRange(0, getParentCount() + 1));
        CommandParts newArguments = new CommandParts(queryReference.getRange(getParentCount() + 1));

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
     * Check whether there's any command description that matches the specified command reference.
     *
     * @param commandReference The command reference.
     *
     * @return True if so, false otherwise.
     */
    public boolean hasSuitableCommand(CommandParts commandReference) {
        return findCommand(commandReference) != null;
    }

    /**
     * Check if the remaining command reference elements are suitable with arguments of the current command description.
     *
     * @param commandReference The command reference.
     *
     * @return True if the arguments are suitable, false otherwise.
     */
    public boolean hasSuitableArguments(CommandParts commandReference) {
        return getSuitableArgumentsDifference(commandReference) == 0;
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
        if (commandReference.getCount() <= 0) {
            return -1;
        }

        // Get the remaining command reference element count
        int remainingElementCount = commandReference.getCount() - getParentCount() - 1;

        // Check if there are too few arguments
        if (getMinimumArguments() > remainingElementCount) {
            return Math.abs(getMinimumArguments() - remainingElementCount);
        }

        // Check if there are too many arguments
        if (getMaximumArguments() < remainingElementCount && getMaximumArguments() >= 0) {
            return Math.abs(remainingElementCount - getMaximumArguments());
        }

        // The arguments seem to be EQUALS, return the result
        return 0;
    }

    /**
     * Get the command permissions.
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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private List<String> labels;
        private String description;
        private String detailedDescription;
        private ExecutableCommand executableCommand;
        private CommandDescription parent;
        private List<CommandDescription> children;
        private List<CommandArgumentDescription> arguments;
        private boolean noArgumentMaximum;
        private CommandPermissions permissions;

        /**
         * Build a CommandDescription from the builder.
         *
         * @return The generated CommandDescription object
         */
        public CommandDescription build() {
            return new CommandDescription(
                valueOrEmptyList(labels),
                firstNonNull(description, ""),
                firstNonNull(detailedDescription, ""),
                firstNonNull(executableCommand, null), // TODO ljacqu 20151128: May `executableCommand` be null?
                firstNonNull(parent, null),
                valueOrEmptyList(children),
                valueOrEmptyList(arguments),
                noArgumentMaximum,
                firstNonNull(permissions, null)
            );
        }

        public Builder labels(List<String> labels) {
            this.labels = labels;
            return this;
        }

        public Builder labels(String... labels) {
            return labels(asMutableList(labels));
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder detailedDescription(String detailedDescription) {
            this.detailedDescription = detailedDescription;
            return this;
        }

        public Builder executableCommand(ExecutableCommand executableCommand) {
            this.executableCommand = executableCommand;
            return this;
        }

        public Builder parent(CommandDescription parent) {
            this.parent = parent;
            return this;
        }

        public Builder children(List<CommandDescription> children) {
            this.children = children;
            return this;
        }

        public Builder children(CommandDescription... children) {
            return children(asMutableList(children));
        }

        public Builder withArgument(String label, String description, boolean isOptional) {
            if (arguments == null) {
                arguments = new ArrayList<>();
            }
            arguments.add(new CommandArgumentDescription(label, description, isOptional));
            return this;
        }

        public Builder noArgumentMaximum(boolean noArgumentMaximum) {
            this.noArgumentMaximum = noArgumentMaximum;
            return this;
        }

        public Builder permissions(CommandPermissions.DefaultPermission defaultPermission,
                                   PermissionNode... permissionNodes) {
            this.permissions = new CommandPermissions(asMutableList(permissionNodes), defaultPermission);
            return this;
        }

        @SafeVarargs
        private static <T> List<T> asMutableList(T... items) {
            return new ArrayList<>(Arrays.asList(items));
        }

        private static <T> List<T> valueOrEmptyList(List<T> givenList) {
            if (givenList != null) {
                return givenList;
            }
            return new ArrayList<>();
        }

        private static <T> T firstNonNull(T first, T second) {
            return first != null ? first : second;
        }
    }

}
