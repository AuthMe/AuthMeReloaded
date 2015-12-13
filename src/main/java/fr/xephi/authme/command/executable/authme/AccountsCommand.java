package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.command.CommandSender;

import java.util.List;

public class AccountsCommand extends ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        final AuthMe plugin = AuthMe.getInstance();
        final Messages m = plugin.getMessages();

        final String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        // Command logic
        if (!playerName.contains(".")) {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    PlayerAuth auth = plugin.database.getAuth(playerName.toLowerCase());
                    if (auth == null) {
                        m.send(sender, MessageKey.UNKNOWN_USER);
                        return;
                    }
                    StringBuilder message = new StringBuilder("[AuthMe] ");
                    List<String> accountList = plugin.database.getAllAuthsByName(auth);
                    if (accountList.isEmpty()) {
                        m.send(sender, MessageKey.USER_NOT_REGISTERED);
                        return;
                    }
                    if (accountList.size() == 1) {
                        sender.sendMessage("[AuthMe] " + playerName + " is a single account player");
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
                    sender.sendMessage("[AuthMe] " + playerName + " has "
                        + String.valueOf(accountList.size()) + " accounts.");
                    sender.sendMessage(message.toString());
                }
            });
            return;
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    List<String> accountList = plugin.database.getAllAuthsByIp(playerName);
                    StringBuilder message = new StringBuilder("[AuthMe] ");
                    if (accountList.isEmpty()) {
                        sender.sendMessage("[AuthMe] This IP does not exist in the database.");
                        return;
                    }
                    if (accountList.size() == 1) {
                        sender.sendMessage("[AuthMe] " + playerName + " is a single account player");
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
                    sender.sendMessage("[AuthMe] " + playerName + " has "
                        + String.valueOf(accountList.size()) + " accounts.");
                    sender.sendMessage(message.toString());
                }
            });
        }
    }
}
