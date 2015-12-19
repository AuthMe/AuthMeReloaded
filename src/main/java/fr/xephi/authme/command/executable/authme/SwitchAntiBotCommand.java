package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AntiBot;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandHandler;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

public class SwitchAntiBotCommand extends ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        if (arguments.isEmpty()) {
            sender.sendMessage("[AuthMe] AntiBot status: " + AntiBot.getAntiBotStatus().name());
            return;
        }

        String newState = arguments.get(0);

        // Enable or disable the mod
        if ("ON".equalsIgnoreCase(newState)) {
            AntiBot.overrideAntiBotStatus(true);
            sender.sendMessage("[AuthMe] AntiBot Manual Override: enabled!");
        } else if ("OFF".equalsIgnoreCase(newState)) {
            AntiBot.overrideAntiBotStatus(false);
            sender.sendMessage("[AuthMe] AntiBotMod Manual Override: disabled!");
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "Invalid AntiBot mode!");
            // TODO ljacqu 20151213: Fix static retrieval of command handler
            CommandHandler commandHandler = AuthMe.getInstance().getCommandHandler();
            FoundCommandResult foundCommandResult =
                commandHandler.mapPartsToCommand(Arrays.asList("authme", "antibot"));
            HelpProvider.printHelp(foundCommandResult, HelpProvider.SHOW_ARGUMENTS);
            sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE + "/authme help antibot");
        }
    }
}
