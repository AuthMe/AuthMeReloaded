package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.EncryptedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.entity.Player;

import java.util.List;

public class RecoverEmailCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        String playerMail = arguments.get(0);
        final String playerName = player.getName();

        // Command logic
        final AuthMe plugin = AuthMe.getInstance();

        if (plugin.mail == null) {
            commandService.send(player, MessageKey.ERROR);
            return;
        }
        DataSource dataSource = commandService.getDataSource();
        if (dataSource.isAuthAvailable(playerName)) {
            if (PlayerCache.getInstance().isAuthenticated(playerName)) {
                commandService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
                return;
            }

            String thePass = RandomString.generate(Settings.getRecoveryPassLength);
            EncryptedPassword hashNew = commandService.getPasswordSecurity().computeHash(thePass, playerName);
            PlayerAuth auth;
            if (PlayerCache.getInstance().isAuthenticated(playerName)) {
                auth = PlayerCache.getInstance().getAuth(playerName);
            } else if (dataSource.isAuthAvailable(playerName)) {
                auth = dataSource.getAuth(playerName);
            } else {
                commandService.send(player, MessageKey.UNKNOWN_USER);
                return;
            }
            if (StringUtils.isEmpty(Settings.getmailAccount)) {
                commandService.send(player, MessageKey.ERROR);
                return;
            }

            if (!playerMail.equalsIgnoreCase(auth.getEmail()) || playerMail.equalsIgnoreCase("your@email.com")
                || auth.getEmail().equalsIgnoreCase("your@email.com")) {
                commandService.send(player, MessageKey.INVALID_EMAIL);
                return;
            }
            auth.setPassword(hashNew);
            dataSource.updatePassword(auth);
            plugin.mail.main(auth, thePass);
            commandService.send(player, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
        } else {
            commandService.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        }
    }
}
