package fr.xephi.authme.commands;

import java.security.NoSuchAlgorithmException;

import me.muizers.Notifications.Notification;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class ChangePasswordCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    private DataSource database;
    public AuthMe plugin;

    public ChangePasswordCommand(DataSource database, AuthMe plugin) {
        this.database = database;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label,
            String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            m._(sender, "no_perm");
            return true;
        }

        Player player = (Player) sender;
        String name = player.getName();
        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            m._(player, "not_logged_in");
            return true;
        }

        if (args.length != 2) {
            m._(player, "usage_changepassword");
            return true;
        }

        try {
            String hashnew = PasswordSecurity.getHash(Settings.getPasswordHash, args[1], name);

            if (PasswordSecurity.comparePasswordWithHash(args[0], PlayerCache.getInstance().getAuth(name).getHash(), player.getName())) {
                PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
                auth.setHash(hashnew);
                if (PasswordSecurity.userSalt.containsKey(name) && PasswordSecurity.userSalt.get(name) != null)
                    auth.setSalt(PasswordSecurity.userSalt.get(name));
                else auth.setSalt("");
                if (!database.updatePassword(auth)) {
                    m._(player, "error");
                    return true;
                }
                database.updateSalt(auth);
                PlayerCache.getInstance().updatePlayer(auth);
                m._(player, "pwd_changed");
                ConsoleLogger.info(player.getName() + " changed his password");
                if (plugin.notifications != null) {
                    plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " change his password!"));
                }
            } else {
                m._(player, "wrong_pwd");
            }
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            m._(sender, "error");
        }
        return true;
    }
}
