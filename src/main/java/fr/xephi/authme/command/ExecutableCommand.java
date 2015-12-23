package fr.xephi.authme.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Base class for AuthMe commands that can be executed.
 */
public interface ExecutableCommand {

    /**
     * Execute the command with the given arguments.
     *
     * @param sender         The command sender.
     * @param arguments      The arguments.
     * @param commandService The command service.
     */
    void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService);

}
