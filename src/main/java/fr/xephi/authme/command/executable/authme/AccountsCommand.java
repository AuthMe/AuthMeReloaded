package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Messages;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

public class AccountsCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(final CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = Messages.getInstance();

        // Get the player query
        String playerQuery = sender.getName();
        if(commandArguments.getCount() >= 1)
            playerQuery = commandArguments.get(0);
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
                        m.send(sender, "unknown_user");
                        return;
                    }
                    if (auth == null) {
                        m.send(sender, "unknown_user");
                        return;
                    }
                    List<String> accountList = plugin.database.getAllAuthsByName(auth);
                    if (accountList == null || accountList.isEmpty()) {
                        m.send(sender, "user_unknown");
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
                            message.append(".");
                        }
                    }
                    sender.sendMessage("[AuthMe] " + playerQueryFinal + " has " + String.valueOf(accountList.size()) + " accounts");
                    sender.sendMessage(message.toString());
                }
            });
            return true;
        } else {
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    StringBuilder message = new StringBuilder("[AuthMe] ");
                    List<String> accountList = plugin.database.getAllAuthsByIp(playerQueryFinal);
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
                            message.append(".");
                        }
                    }
                    sender.sendMessage("[AuthMe] " + playerQueryFinal + " has " + String.valueOf(accountList.size()) + " accounts");
                    sender.sendMessage(message.toString());
                }
            });
            return true;
        }
    }
}
