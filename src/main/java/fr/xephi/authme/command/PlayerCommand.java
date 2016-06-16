package fr.xephi.authme.command;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Common base type for player-only commands, handling the verification that the command sender is indeed a player.
 */
public abstract class PlayerCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        if (sender instanceof Player) {
            runCommand((Player) sender, arguments);
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
     * Runs the command with the given player and arguments.
     *
     * @param player     the player who initiated the command
     * @param arguments  the arguments supplied with the command
     */
    protected abstract void runCommand(Player player, List<String> arguments);

    /**
     * Returns an alternative command (textual representation) that is not restricted to players only.
     * Example: {@code "/authme register <playerName> <password>"}
     *
     * @return Alternative command not restricted to players, or null if not applicable
     */
    protected String getAlternativeCommand() {
        return null;
    }

}
