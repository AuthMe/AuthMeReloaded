package fr.xephi.authme.process.changepassword;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.entity.Player;

import javax.inject.Inject;

public class AsyncChangePassword implements AsynchronousProcess {

    @Inject
    private DataSource dataSource;

    @Inject
    private ProcessService processService;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private PlayerCache playerCache;

    AsyncChangePassword() { }


    public void changePassword(final Player player, String oldPassword, String newPassword) {
        final String name = player.getName().toLowerCase();
        PlayerAuth auth = playerCache.getAuth(name);
        if (passwordSecurity.comparePassword(oldPassword, auth.getPassword(), player.getName())) {
            HashedPassword hashedPassword = passwordSecurity.computeHash(newPassword, name);
            auth.setPassword(hashedPassword);

            if (!dataSource.updatePassword(auth)) {
                processService.send(player, MessageKey.ERROR);
                return;
            }

            playerCache.updatePlayer(auth);
            processService.send(player, MessageKey.PASSWORD_CHANGED_SUCCESS);
            ConsoleLogger.info(player.getName() + " changed his password");
        } else {
            processService.send(player, MessageKey.WRONG_PASSWORD);
        }
    }
}

