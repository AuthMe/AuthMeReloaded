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
            bukkitService.runTaskAsynchronously(() -> {
                List<String> accountList = dataSource.getAllAuthsByIp(playerName);
                if (accountList.isEmpty()) {
                    commonService.send(sender, MessageKey.ACCOUNTS_IP_NOT_FOUND);
                } else if (accountList.size() == 1) {
                    commonService.send(sender, MessageKey.ACCOUNTS_SINGLE, playerName);
                } else {
                    outputAccountsList(sender, playerName, accountList);
                }
            });
        } else {
            bukkitService.runTaskAsynchronously(() -> {
                PlayerAuth auth = dataSource.getAuth(playerName.toLowerCase(Locale.ROOT));
                if (auth == null) {
                    commonService.send(sender, MessageKey.UNKNOWN_USER);
                    return;
                } else if (auth.getLastIp() == null) {
                    commonService.send(sender, MessageKey.ACCOUNTS_NO_LAST_IP);
                    return;
                }

                List<String> accountList = dataSource.getAllAuthsByIp(auth.getLastIp());
                if (accountList.isEmpty()) {
                    commonService.send(sender, MessageKey.UNKNOWN_USER);
                } else if (accountList.size() == 1) {
                    commonService.send(sender, MessageKey.ACCOUNTS_SINGLE, playerName);
                } else {
                    outputAccountsList(sender, playerName, accountList);
                }
            });
        }
    }

    private void outputAccountsList(CommandSender sender, String playerName, List<String> accountList) {
        if (playerName.equalsIgnoreCase(sender.getName())) {
            commonService.send(sender, MessageKey.ACCOUNTS_OWNED_SELF, Integer.toString(accountList.size()));
        } else {
            commonService.send(sender, MessageKey.ACCOUNTS_OWNED_OTHER, playerName, Integer.toString(accountList.size()));
        }
        sender.sendMessage(String.join(", ", accountList));
    }
}
