package fr.xephi.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.settings.Messages;

public class LoginCommand implements CommandExecutor {

    private AuthMe plugin;
    private Messages m = Messages.getInstance();

    public LoginCommand(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label,
            final String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        final Player player = (Player) sender;

        if (args.length == 0) {
            m._(player, "usage_log");
            return true;
        }

        if (!plugin.authmePermissible(player, "authme." + label.toLowerCase())) {
            m._(player, "no_perm");
            return true;
        }
        plugin.management.performLogin(player, args[0], false);
        return true;
    }
}
