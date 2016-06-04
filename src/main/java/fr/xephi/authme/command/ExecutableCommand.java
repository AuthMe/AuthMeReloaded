package fr.xephi.authme.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Base class for AuthMe commands that can be executed.
 */
public interface ExecutableCommand {

    /**
     * Executes the command with the given arguments.
     *
     * @param sender     the command sender (initiator of the command)
     * @param arguments  the arguments
     */
    void executeCommand(CommandSender sender, List<String> arguments);

}
