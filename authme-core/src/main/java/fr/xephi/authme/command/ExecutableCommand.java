package fr.xephi.authme.command;

import fr.xephi.authme.message.MessageKey;
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

    /**
     * Returns the message to show to the user if the command is used with the wrong arguments.
     * If null is returned, the standard help (/<i>command</i> help) output is shown.
     *
     * @return the message explaining the command's usage, or {@code null} for default behavior
     */
    default MessageKey getArgumentsMismatchMessage() {
        return null;
    }

}
