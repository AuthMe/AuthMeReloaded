package fr.xephi.authme.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Common base type for player-only commands, handling the verification that the command sender is indeed a player.
 */
public abstract class PlayerCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        if (sender instanceof Player) {
            runCommand((Player) sender, arguments, commandService);
        } else {
            String alternative = getAlternativeCommand();
            if (alternative != null) {
                sender.sendMessage("Player only! Please use " + alternative + " instead.");
            } else {
                sender.sendMessage("This command is only for players.");
            }
        }
    }

    /**
     * Run the command with the given player and arguments.
     *
     * @param player         The player who initiated the command
     * @param arguments      The arguments supplied with the command
     * @param commandService The command service
     */
    protected abstract void runCommand(Player player, List<String> arguments, CommandService commandService);

    /**
     * Return an alternative command (textual representation) that is not restricted to players only.
     * Example: "authme register &lt;playerName> &lt;password>"
     *
     * @return Alternative command not only for players, or null if not applicable
     */
    protected String getAlternativeCommand() {
        return null;
    }

}
