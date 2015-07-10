package fr.xephi.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.settings.Messages;

public class LogoutCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    private AuthMe plugin;

    public LogoutCommand(AuthMe plugin) {
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

        final Player player = (Player) sender;
        plugin.management.performLogout(player);
        return true;
    }

}
