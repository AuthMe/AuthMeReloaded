package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AntiBot;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.help.HelpProvider;
import fr.xephi.authme.util.CollectionUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class SwitchAntiBotCommand extends ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // Get the new state
        String newState = null;

        if (arguments.size() == 1) {
            newState = arguments.get(0);
        } else if (arguments.size() == 0) {
            sender.sendMessage("[AuthMe] AntiBot status: " + AntiBot.getAntiBotStatus().name());
            return;
        }

        // Enable the mod
        if ("ON".equalsIgnoreCase(newState)) {
            AntiBot.overrideAntiBotStatus(true);
            sender.sendMessage("[AuthMe] AntiBot Manual Override: enabled!");
            return;
        }

        // Disable the mod
        if ("OFF".equalsIgnoreCase(newState)) {
            AntiBot.overrideAntiBotStatus(false);
            sender.sendMessage("[AuthMe] AntiBotMod Manual Override: disabled!");
            return;
        }

        // Show the invalid arguments warning
        sender.sendMessage(ChatColor.DARK_RED + "Invalid AntiBot mode!");

        // Show the command argument help
        // FIXME fix help reference
        HelpProvider.showHelp(sender, commandReference, commandReference, true, false, true, false, false, false);

        // Show the command to use for detailed help
        List<String> helpCommandReference = CollectionUtils.getRange(commandReference.getList(), 1);
        sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE + "/"
            + commandReference.get(0) + " help " + CommandUtils.labelsToString(helpCommandReference));
    }
}
