package fr.xephi.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Messages;

/**
 *
 * @author stefano
 */
public class PasspartuCommand implements CommandExecutor {

    private Utils utils = Utils.getInstance();
    public AuthMe plugin;
    private Messages m = Messages.getInstance();

    public PasspartuCommand(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label,
            String[] args) {

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            m._(sender, "no_perm");
            return true;
        }

        if (PlayerCache.getInstance().isAuthenticated(sender.getName())) {
            return true;
        }

        if ((sender instanceof Player) && args.length == 1) {
            if (utils.readToken(args[0])) {
                // bypass login!
                plugin.management.performLogin((Player) sender, "dontneed", true);
                return true;
            }
            sender.sendMessage("Time is expired or Token is Wrong!");
            return true;
        }
        sender.sendMessage("usage: /passpartu token");
        return true;
    }
}
