package fr.xephi.authme.command.executable.premium;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.PremiumService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command to enable premium mode for a player with an official Minecraft account.
 */
public class PremiumCommand extends PlayerCommand {

    @Inject
    private PremiumService premiumService;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        premiumService.enablePremium(player);
    }

    @Override
    public MessageKey getArgumentsMismatchMessage() {
        return MessageKey.UNKNOWN_COMMAND;
    }
}
