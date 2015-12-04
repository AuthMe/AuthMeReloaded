package fr.xephi.authme.command;

import org.bukkit.command.CommandSender;

/**
 */
public class FoundCommandResult {

    /**
     * The command description instance.
     */
    private CommandDescription commandDescription;
    /**
     * The command reference.
     */
    private final CommandParts commandReference;
    /**
     * The command arguments.
     */
    private final CommandParts commandArguments;
    /**
     * The original search query reference.
     */
    private final CommandParts queryReference;

    /**
     * Constructor.
     *
     * @param commandDescription The command description.
     * @param commandReference   The command reference.
     * @param commandArguments   The command arguments.
     * @param queryReference     The original query reference.
     */
    public FoundCommandResult(CommandDescription commandDescription, CommandParts commandReference, CommandParts commandArguments, CommandParts queryReference) {
        this.commandDescription = commandDescription;
        this.commandReference = commandReference;
        this.commandArguments = commandArguments;
        this.queryReference = queryReference;
    }

    /**
     * Check whether the command was suitable.
     *
     * @return True if the command was suitable, false otherwise.
     */
    public boolean hasProperArguments() {
        // Make sure the command description is set
        if (this.commandDescription == null)
            return false;

        // Get and return the result
        return getCommandDescription().getSuitableArgumentsDifference(this.queryReference) == 0;
    }

    /**
     * Get the command description.
     *
     * @return Command description.
     */
    public CommandDescription getCommandDescription() {
        return this.commandDescription;
    }

    /**
     * Check whether the command is executable.
     *
     * @return True if the command is executable, false otherwise.
     */
    public boolean isExecutable() {
        return commandDescription != null;
    }

    /**
     * Execute the command.
     *
     * @param sender The command sender that executed the command.
     *
     * @return True on success, false on failure.
     */
    public boolean executeCommand(CommandSender sender) {
        // Make sure the command description is valid
        if (this.commandDescription == null)
            return false;

        // Execute the command
        return this.commandDescription.execute(sender, this.commandReference, this.commandArguments);
    }

    /**
     * Check whether a command sender has permission to execute the command.
     *
     * @param sender The command sender.
     *
     * @return True if the command sender has permission, false otherwise.
     */
    public boolean hasPermission(CommandSender sender) {
        if (commandDescription == null) {
            return false;
        } else if (commandDescription.getCommandPermissions() == null) {
            return true;
        }

        // TODO: Move permissions check to the permission package; command package should not define permission-checking
        // API
        return commandDescription.getCommandPermissions().hasPermission(sender);
    }

    /**
     * Get the command reference.
     *
     * @return The command reference.
     */
    public CommandParts getCommandReference() {
        return this.commandReference;
    }

    /**
     * Get the command arguments.
     *
     * @return The command arguments.
     */
    public CommandParts getCommandArguments() {
        return this.commandArguments;
    }

    /**
     * Get the original query reference.
     *
     * @return Original query reference.
     */
    public CommandParts getQueryReference() {
        return this.queryReference;
    }

    /**
     * Get the difference value between the original query and the result reference.
     *
     * @return The difference value.
     */
    public double getDifference() {
        // Get the difference through the command found
        if (this.commandDescription != null) {
            return this.commandDescription.getCommandDifference(this.queryReference);
        }

        // Get the difference from the query reference
        return CommandUtils.getDifference(queryReference.getList(), commandReference.getList(), true);
    }
}
