package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.service.PremiumService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Admin command to enable premium mode for a player by name.
 */
public class SetPremiumAdminCommand implements ExecutableCommand {

    @Inject
    private PremiumService premiumService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        premiumService.enablePremiumAdmin(sender, arguments.get(0));
    }
}
