package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Messages;
import org.bukkit.command.CommandSender;

import java.util.Date;

public class LastLoginCommand extends ExecutableCommand {

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
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        AuthMe plugin = AuthMe.getInstance();
        // Messages instance
        Messages m = Messages.getInstance();

        // Get the player
        String playerName = sender.getName();
        if(commandArguments.getCount() >= 1)
            playerName = commandArguments.get(0);

        // Validate the player
        PlayerAuth auth;
        try {
            auth = plugin.database.getAuth(playerName.toLowerCase());
        } catch (NullPointerException e) {
            m.send(sender, "unknown_user");
            return true;
        }
        if (auth == null) {
            m.send(sender, "user_unknown");
            return true;
        }

        // Get the last login date
        long lastLogin = auth.getLastLogin();
        Date date = new Date(lastLogin);

        // Get the difference
        final long diff = System.currentTimeMillis() - lastLogin;

        // Build the message
        final String msg = (int) (diff / 86400000) + " days " + (int) (diff / 3600000 % 24) + " hours " + (int) (diff / 60000 % 60) + " mins " + (int) (diff / 1000 % 60) + " secs.";

        // Get the player's last IP
        String lastIP = auth.getIp();

        // Show the player status
        sender.sendMessage("[AuthMe] " + playerName + " last login : " + date.toString());
        sender.sendMessage("[AuthMe] The player " + auth.getNickname() + " is unlogged since " + msg);
        sender.sendMessage("[AuthMe] Last Player's IP: " + lastIP);
        return true;
    }
}
