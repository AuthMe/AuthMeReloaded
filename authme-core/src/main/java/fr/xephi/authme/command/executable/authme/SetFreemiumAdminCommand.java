package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.service.PremiumService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Admin command to disable premium mode for a player by name.
 */
public class SetFreemiumAdminCommand implements ExecutableCommand {

    @Inject
    private PremiumService premiumService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        premiumService.disablePremiumAdmin(sender, arguments.get(0));
    }
}
