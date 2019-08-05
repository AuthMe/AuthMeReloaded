package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Command to see whether a user has enabled two-factor authentication.
 */
public class TotpViewStatusCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private Messages messages;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        String player = arguments.get(0);

        PlayerAuth auth = dataSource.getAuth(player);
        if (auth == null) {
            messages.send(sender, MessageKey.UNKNOWN_USER);
        } else if (auth.getTotpKey() == null) {
            sender.sendMessage(ChatColor.RED + "Player '" + player + "' does NOT have two-factor auth enabled");
        } else {
            sender.sendMessage(ChatColor.DARK_GREEN + "Player '" + player + "' has enabled two-factor authentication");
        }
    }
}
