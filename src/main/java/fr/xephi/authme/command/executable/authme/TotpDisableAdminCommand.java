package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command to disable two-factor authentication for a user.
 */
public class TotpDisableAdminCommand implements ExecutableCommand {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(TotpDisableAdminCommand.class);

    @Inject
    private DataSource dataSource;

    @Inject
    private Messages messages;

    @Inject
    private BukkitService bukkitService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        String player = arguments.get(0);

        PlayerAuth auth = dataSource.getAuth(player);
        if (auth == null) {
            messages.send(sender, MessageKey.UNKNOWN_USER);
        } else if (auth.getTotpKey() == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + player + "' does not have two-factor auth enabled");
        } else {
            removeTotpKey(sender, player);
        }
    }

    private void removeTotpKey(CommandSender sender, String player) {
        if (dataSource.removeTotpKey(player)) {
            sender.sendMessage("Disabled two-factor authentication successfully for '" + player + "'");
            logger.info(sender.getName() + " disable two-factor authentication for '" + player + "'");

            Player onlinePlayer = bukkitService.getPlayerExact(player);
            if (onlinePlayer != null) {
                messages.send(onlinePlayer, MessageKey.TWO_FACTOR_REMOVED_SUCCESS);
            }
        } else {
            messages.send(sender, MessageKey.ERROR);
        }
    }
}
