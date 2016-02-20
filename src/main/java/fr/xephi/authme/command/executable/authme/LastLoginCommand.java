package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
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

        PlayerAuth auth = commandService.getDataSource().getAuth(playerName.toLowerCase());
        if (auth == null) {
            commandService.send(sender, MessageKey.USER_NOT_REGISTERED);
            return;
        }

        // Get the last login date
        long lastLogin = auth.getLastLogin();
        final long diff = System.currentTimeMillis() - lastLogin;
        final String lastLoginMessage = (int) (diff / 86400000) + " days " + (int) (diff / 3600000 % 24) + " hours "
            + (int) (diff / 60000 % 60) + " mins " + (int) (diff / 1000 % 60) + " secs";
        Date date = new Date(lastLogin);

        // Show the player status
        sender.sendMessage("[AuthMe] " + playerName + " last login: " + date.toString());
        sender.sendMessage("[AuthMe] The player " + auth.getNickname() + " last logged in "
            + lastLoginMessage + " ago.");
        sender.sendMessage("[AuthMe] Last Player's IP: " + auth.getIp());
    }
}
