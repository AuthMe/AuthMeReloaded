package fr.xephi.authme.commands;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.ChangePasswordTask;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChangePasswordCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    public AuthMe plugin;

    public ChangePasswordCommand(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label,
                             String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            m.send(sender, "no_perm");
            return true;
        }

        Player player = (Player) sender;
        String name = player.getName().toLowerCase();
        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, "not_logged_in");
            return true;
        }

        if (args.length != 2) {
            m.send(player, "usage_changepassword");
            return true;
        }

        String lowpass = args[1].toLowerCase();
        if (lowpass.contains("delete") || lowpass.contains("where") || lowpass.contains("insert") || lowpass.contains("modify") || lowpass.contains("from") || lowpass.contains("select") || lowpass.contains(";") || lowpass.contains("null") || !lowpass.matches(Settings.getPassRegex)) {
            m.send(player, "password_error");
            return true;
        }
        if (lowpass.equalsIgnoreCase(name)) {
            m.send(player, "password_error_nick");
            return true;
        }
        if (lowpass.length() < Settings.getPasswordMinLen || lowpass.length() > Settings.passwordMaxLength) {
            m.send(player, "pass_len");
            return true;
        }
        if (!Settings.unsafePasswords.isEmpty()) {
            if (Settings.unsafePasswords.contains(lowpass)) {
                m.send(player, "password_error_unsafe");
                return true;
            }
        }
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new ChangePasswordTask(plugin, player, args[0]));
        return true;
    }
}
