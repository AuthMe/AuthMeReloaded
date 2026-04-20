package fr.xephi.authme.process.changepassword;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Locale;

public class AsyncChangePassword implements AsynchronousProcess {
    
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(AsyncChangePassword.class);

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private PlayerCache playerCache;

    AsyncChangePassword() {
    }

    /**
     * Change password for an online player
     *
     * @param player the player
     * @param oldPassword the old password used by the player
     * @param newPassword the new password chosen by the player
     */
    public void changePassword(Player player, String oldPassword, String newPassword) {
        String name = player.getName().toLowerCase(Locale.ROOT);
        PlayerAuth auth = playerCache.getAuth(name);
        if (passwordSecurity.comparePassword(oldPassword, auth.getPassword(), player.getName())) {
            HashedPassword hashedPassword = passwordSecurity.computeHash(newPassword, name);
            auth.setPassword(hashedPassword);

            if (!dataSource.updatePassword(auth)) {
                commonService.send(player, MessageKey.ERROR);
                return;
            }

            // TODO: send an update when a messaging service will be implemented (PASSWORD_CHANGED)

            playerCache.updatePlayer(auth);
            commonService.send(player, MessageKey.PASSWORD_CHANGED_SUCCESS);
            logger.info(player.getName() + " changed his password");
        } else {
            commonService.send(player, MessageKey.WRONG_PASSWORD);
        }
    }

    /**
     * Change a user's password as an administrator, without asking for the previous one
     *
     * @param sender who is performing the operation, null if called by other plugins
     * @param playerName the player name
     * @param newPassword the new password chosen for the player
     */
    public void changePasswordAsAdmin(CommandSender sender, String playerName, String newPassword) {
        String lowerCaseName = playerName.toLowerCase(Locale.ROOT);
        if (!(playerCache.isAuthenticated(lowerCaseName) || dataSource.isAuthAvailable(lowerCaseName))) {
            if (sender == null) {
                logger.warning("Tried to change password for user " + lowerCaseName + " but it doesn't exist!");
            } else {
                commonService.send(sender, MessageKey.UNKNOWN_USER);
            }
            return;
        }

        HashedPassword hashedPassword = passwordSecurity.computeHash(newPassword, lowerCaseName);
        if (dataSource.updatePassword(lowerCaseName, hashedPassword)) {
            // TODO: send an update when a messaging service will be implemented (PASSWORD_CHANGED)

            if (sender != null) {
                commonService.send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
                logger.info(sender.getName() + " changed password of " + lowerCaseName);
            } else {
                logger.info("Changed password of " + lowerCaseName);
            }
        } else {
            if (sender != null) {
                commonService.send(sender, MessageKey.ERROR);
            }
            logger.warning("An error occurred while changing password for user " + lowerCaseName + "!");
        }
    }
}
