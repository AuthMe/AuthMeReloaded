package fr.xephi.authme.task;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;

import java.security.NoSuchAlgorithmException;

public class ChangePasswordTask implements Runnable {

    private final AuthMe plugin;
    private final Player player;
    private String password;

    public ChangePasswordTask(AuthMe plugin, Player player, String password) {
        this.plugin = plugin;
        this.player = player;
        this.password = password;
    }

    @Override
    public void run() {
        Messages m = Messages.getInstance();
        try {
            String name = player.getName().toLowerCase();
            String hashnew = PasswordSecurity.getHash(Settings.getPasswordHash, password, name);
            if (PasswordSecurity.comparePasswordWithHash(password, PlayerCache.getInstance().getAuth(name).getHash(), player.getName())) {
                PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
                auth.setHash(hashnew);
                if (PasswordSecurity.userSalt.containsKey(name) && PasswordSecurity.userSalt.get(name) != null)
                    auth.setSalt(PasswordSecurity.userSalt.get(name));
                else auth.setSalt("");
                if (!plugin.database.updatePassword(auth)) {
                    m.send(player, "error");
                    return;
                }
                plugin.database.updateSalt(auth);
                PlayerCache.getInstance().updatePlayer(auth);
                m.send(player, "pwd_changed");
                ConsoleLogger.info(player.getName() + " changed his password");
            } else {
                m.send(player, "wrong_pwd");
            }
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            m.send(player, "error");
        }
    }
}

