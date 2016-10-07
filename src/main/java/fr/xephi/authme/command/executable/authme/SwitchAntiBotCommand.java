package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.service.AntiBotService;
import fr.xephi.authme.command.CommandMapper;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.command.FoundCommandResult;
import fr.xephi.authme.command.help.HelpProvider;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * Display or change the status of the antibot mod.
 */
public class SwitchAntiBotCommand implements ExecutableCommand {

    @Inject
    private AntiBotService antiBotService;

    @Inject
    private CommandMapper commandMapper;

    @Inject
    private HelpProvider helpProvider;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        if (arguments.isEmpty()) {
            sender.sendMessage("[AuthMe] AntiBot status: " + antiBotService.getAntiBotStatus().name());
            return;
        }

        String newState = arguments.get(0);

        // Enable or disable the mod
        if ("ON".equalsIgnoreCase(newState)) {
            antiBotService.overrideAntiBotStatus(true);
            sender.sendMessage("[AuthMe] AntiBot Manual Override: enabled!");
        } else if ("OFF".equalsIgnoreCase(newState)) {
            antiBotService.overrideAntiBotStatus(false);
            sender.sendMessage("[AuthMe] AntiBot Manual Override: disabled!");
        } else {
            sender.sendMessage(ChatColor.DARK_RED + "Invalid AntiBot mode!");
            FoundCommandResult result = commandMapper.mapPartsToCommand(sender, Arrays.asList("authme", "antibot"));
            helpProvider.outputHelp(sender, result, HelpProvider.SHOW_ARGUMENTS);
            sender.sendMessage(ChatColor.GOLD + "Detailed help: " + ChatColor.WHITE + "/authme help antibot");
        }
    }
}
