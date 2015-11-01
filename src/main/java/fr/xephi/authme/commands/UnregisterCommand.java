package fr.xephi.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Messages;

public class UnregisterCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    public AuthMe plugin;

    public UnregisterCommand(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(final CommandSender sender, Command cmnd, String label,
                             final String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            m.send(sender, "no_perm");
            return true;
        }

        final Player player = (Player) sender;
        final String name = player.getName().toLowerCase();

        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, "not_logged_in");
            return true;
        }

        if (args.length != 1) {
            m.send(player, "usage_unreg");
            return true;
        }

        plugin.management.performUnregister(player, args[0], false);
        return true;
    }
}
