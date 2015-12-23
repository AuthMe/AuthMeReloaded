package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.command.CommandSender;

import java.util.Date;
import java.util.List;

/**
 */
public class LastLoginCommand implements ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments, CommandService commandService) {
        // Get the player
        String playerName = (arguments.size() >= 1) ? arguments.get(0) : sender.getName();

        // Validate the player
        AuthMe plugin = AuthMe.getInstance();
        Messages m = plugin.getMessages();

        PlayerAuth auth;
        try {
            auth = plugin.database.getAuth(playerName.toLowerCase());
        } catch (NullPointerException e) {
            m.send(sender, MessageKey.UNKNOWN_USER);
            return;
        }
        if (auth == null) {
            m.send(sender, MessageKey.USER_NOT_REGISTERED);
            return;
        }

        // Get the last login date
        long lastLogin = auth.getLastLogin();
        Date date = new Date(lastLogin);

        // Get the difference
        final long diff = System.currentTimeMillis() - lastLogin;

        // Build the message
        final String msg = (int) (diff / 86400000) + " days " + (int) (diff / 3600000 % 24) + " hours "
            + (int) (diff / 60000 % 60) + " mins " + (int) (diff / 1000 % 60) + " secs.";

        // Get the player's last IP
        String lastIP = auth.getIp();

        // Show the player status
        sender.sendMessage("[AuthMe] " + playerName + " last login : " + date.toString());
        sender.sendMessage("[AuthMe] The player " + auth.getNickname() + " is unlogged since " + msg);
        sender.sendMessage("[AuthMe] Last Player's IP: " + lastIP);
    }
}
