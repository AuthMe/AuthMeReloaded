package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

public class AccountsCommand extends ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        final AuthMe plugin = AuthMe.getInstance();
        final Messages m = plugin.getMessages();

        // Get the player query
        String playerQuery = sender.getName();
        if (arguments.size() >= 1)
            playerQuery = arguments.get(0);
        final String playerQueryFinal = playerQuery;

        // Command logic
        if (!playerQuery.contains(".")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {
                    PlayerAuth auth;
                    StringBuilder message = new StringBuilder("[AuthMe] ");
                    try {
                        auth = plugin.database.getAuth(playerQueryFinal.toLowerCase());
                    } catch (NullPointerException npe) {
                        m.send(sender, MessageKey.UNKNOWN_USER);
                        return;
                    }
                    if (auth == null) {
                        m.send(sender, MessageKey.UNKNOWN_USER);
                        return;
                    }
                    List<String> accountList = plugin.database.getAllAuthsByName(auth);
                    if (accountList == null || accountList.isEmpty()) {
                        m.send(sender, MessageKey.USER_NOT_REGISTERED);
                        return;
                    }
                    if (accountList.size() == 1) {
                        sender.sendMessage("[AuthMe] " + playerQueryFinal + " is a single account player");
                        return;
                    }
                    int i = 0;
                    for (String account : accountList) {
                        i++;
                        message.append(account);
                        if (i != accountList.size()) {
                            message.append(", ");
                        } else {
                            message.append('.');
                        }
                    }
                    sender.sendMessage("[AuthMe] " + playerQueryFinal + " has " + String.valueOf(accountList.size()) + " accounts");
                    sender.sendMessage(message.toString());
                }
            });
            return;
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    List<String> accountList;
                    try {
                        accountList = plugin.database.getAllAuthsByIp(playerQueryFinal);
                    } catch (Exception e) {
                        ConsoleLogger.showError(e.getMessage());
                        ConsoleLogger.writeStackTrace(e);
                        m.send(sender, MessageKey.ERROR);
                        return;
                    }

                    StringBuilder message = new StringBuilder("[AuthMe] ");
                    if (accountList == null || accountList.isEmpty()) {
                        sender.sendMessage("[AuthMe] This IP does not exist in the database");
                        return;
                    }
                    if (accountList.size() == 1) {
                        sender.sendMessage("[AuthMe] " + playerQueryFinal + " is a single account player");
                        return;
                    }
                    int i = 0;
                    for (String account : accountList) {
                        i++;
                        message.append(account);
                        if (i != accountList.size()) {
                            message.append(", ");
                        } else {
                            message.append('.');
                        }
                    }
                    sender.sendMessage("[AuthMe] " + playerQueryFinal + " has " + String.valueOf(accountList.size()) + " accounts");
                    sender.sendMessage(message.toString());
                }
            });
        }
    }
}
