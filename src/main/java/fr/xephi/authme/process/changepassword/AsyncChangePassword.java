package fr.xephi.authme.process.changepassword;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.service.bungeecord.MessageType;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;

public class AsyncChangePassword implements AsynchronousProcess {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private BungeeSender bungeeSender;

    AsyncChangePassword() {
    }

    /**
     * Change password for an online player
     *
     * @param player the player
     * @param oldPassword the old password used by the player
     * @param newPassword the new password chosen by the player
     */
    public void changePassword(final Player player, String oldPassword, String newPassword) {
        final String name = player.getName().toLowerCase();
        PlayerAuth auth = playerCache.getAuth(name);
        if (passwordSecurity.comparePassword(oldPassword, auth.getPassword(), player.getName())) {
            HashedPassword hashedPassword = passwordSecurity.computeHash(newPassword, name);
            auth.setPassword(hashedPassword);

            if (!dataSource.updatePassword(auth)) {
                commonService.send(player, MessageKey.ERROR);
                return;
            }
            bungeeSender.sendAuthMeBungeecordMessage(MessageType.REFRESH_PASSWORD, name);

            playerCache.updatePlayer(auth);
            commonService.send(player, MessageKey.PASSWORD_CHANGED_SUCCESS);
            ConsoleLogger.info(player.getName() + " changed his password");
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
    public void changePasswordAsAdmin(CommandSender sender, final String playerName, String newPassword) {
        final String lowerCaseName = playerName.toLowerCase();
        if (!(playerCache.isAuthenticated(lowerCaseName) || dataSource.isAuthAvailable(lowerCaseName))) {
            if (sender == null) {
                ConsoleLogger.warning("Tried to change password for user " + lowerCaseName + " but it doesn't exist!");
            } else {
                commonService.send(sender, MessageKey.UNKNOWN_USER);
            }
            return;
        }

        HashedPassword hashedPassword = passwordSecurity.computeHash(newPassword, lowerCaseName);
        if (dataSource.updatePassword(lowerCaseName, hashedPassword)) {
            bungeeSender.sendAuthMeBungeecordMessage(MessageType.REFRESH_PASSWORD, lowerCaseName);
            if (sender != null) {
                commonService.send(sender, MessageKey.PASSWORD_CHANGED_SUCCESS);
                ConsoleLogger.info(sender.getName() + " changed password of " + lowerCaseName);
            } else {
                ConsoleLogger.info("Changed password of " + lowerCaseName);
            }
        } else {
            if (sender != null) {
                commonService.send(sender, MessageKey.ERROR);
            }
            ConsoleLogger.warning("An error occurred while changing password for user " + lowerCaseName + "!");
        }
    }
}
