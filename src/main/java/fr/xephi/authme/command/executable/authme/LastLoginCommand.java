package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

/**
 * Returns the last login date of the given user.
 */
public class LastLoginCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Get the player
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        PlayerAuth auth = dataSource.getAuth(playerName);
        if (auth == null) {
            commonService.send(sender, MessageKey.UNKNOWN_USER);
            return;
        }

        // Get the last login date
        final Long lastLogin = auth.getLastLogin();
        final String lastLoginDate = lastLogin == null ? "never" : new Date(lastLogin).toString();

        // Show the player status
        sender.sendMessage("[AuthMe] " + playerName + " last login: " + lastLoginDate);
        if (lastLogin != null) {
            sender.sendMessage("[AuthMe] The player " + playerName + " last logged in "
                + createLastLoginIntervalMessage(lastLogin) + " ago");
        }
        sender.sendMessage("[AuthMe] Last player's IP: " + auth.getLastIp());
    }

    private static String createLastLoginIntervalMessage(long lastLogin) {
        final long diff = System.currentTimeMillis() - lastLogin;
        return (int) (diff / 86400000) + " days "
            + (int) (diff / 3600000 % 24) + " hours "
            + (int) (diff / 60000 % 60) + " mins "
            + (int) (diff / 1000 % 60) + " secs";
    }
}
