package fr.xephi.authme.task;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;

import java.security.NoSuchAlgorithmException;

/**
 */
public class ChangePasswordTask implements Runnable {

    private final AuthMe plugin;
    private final Player player;
    private final String oldPassword;
    private final String newPassword;

    /**
     * Constructor for ChangePasswordTask.
     *
     * @param plugin      AuthMe
     * @param player      Player
     * @param oldPassword String
     * @param newPassword String
     */
    public ChangePasswordTask(AuthMe plugin, Player player, String oldPassword, String newPassword) {
        this.plugin = plugin;
        this.player = player;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    @Override
    public void run() {
        Messages m = plugin.getMessages();
        try {
            String name = player.getName().toLowerCase();
            String hashNew = PasswordSecurity.getHash(Settings.getPasswordHash, newPassword, name);
            PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
            if (PasswordSecurity.comparePasswordWithHash(oldPassword, auth.getHash(), player.getName())) {
                auth.setHash(hashNew);
                if (PasswordSecurity.userSalt.containsKey(name) && PasswordSecurity.userSalt.get(name) != null) {
                    auth.setSalt(PasswordSecurity.userSalt.get(name));
                } else {
                    auth.setSalt("");
                }
                if (!plugin.database.updatePassword(auth)) {
                    m.send(player, MessageKey.ERROR);
                    return;
                }
                plugin.database.updateSalt(auth);
                PlayerCache.getInstance().updatePlayer(auth);
                m.send(player, MessageKey.PASSWORD_CHANGED_SUCCESS);
                ConsoleLogger.info(player.getName() + " changed his password");
            } else {
                m.send(player, MessageKey.WRONG_PASSWORD);
            }
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            m.send(player, MessageKey.ERROR);
        }
    }
}

