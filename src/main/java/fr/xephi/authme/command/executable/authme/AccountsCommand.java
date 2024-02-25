package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

/**
 * Shows all accounts registered by the same IP address for the given player name or IP address.
 */
public class AccountsCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private CommonService commonService;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // TODO #1366: last IP vs. registration IP?
        final String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        // Assumption: a player name cannot contain '.'
        if (playerName.contains(".")) {
            bukkitService.runOnAsyncSchedulerNow(task -> {
                List<String> accountList = dataSource.getAllAuthsByIp(playerName);
                if (accountList.isEmpty()) {
                    sender.sendMessage("[AuthMe] This IP does not exist in the database.");
                } else if (accountList.size() == 1) {
                    sender.sendMessage("[AuthMe] " + playerName + " is a single account player");
                } else {
                    outputAccountsList(sender, playerName, accountList);
                }
            });
        } else {
            bukkitService.runOnAsyncSchedulerNow(task -> {
                PlayerAuth auth = dataSource.getAuth(playerName.toLowerCase(Locale.ROOT));
                if (auth == null) {
                    commonService.send(sender, MessageKey.UNKNOWN_USER);
                    return;
                } else if (auth.getLastIp() == null) {
                    sender.sendMessage("No known last IP address for player");
                    return;
                }

                List<String> accountList = dataSource.getAllAuthsByIp(auth.getLastIp());
                if (accountList.isEmpty()) {
                    commonService.send(sender, MessageKey.UNKNOWN_USER);
                } else if (accountList.size() == 1) {
                    sender.sendMessage("[AuthMe] " + playerName + " is a single account player");
                } else {
                    outputAccountsList(sender, playerName, accountList);
                }
            });
        }
    }

    private static void outputAccountsList(CommandSender sender, String playerName, List<String> accountList) {
        sender.sendMessage("[AuthMe] " + playerName + " has " + accountList.size() + " accounts.");
        String message = "[AuthMe] " + String.join(", ", accountList) + ".";
        sender.sendMessage(message);
    }
}
