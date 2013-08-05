/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.org.whoami.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.Utils;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.settings.Messages;

/**
 *
 * @author stefano
 */
public class PasspartuCommand implements CommandExecutor {
    private Utils utils = new Utils();
    public AuthMe plugin;
	private Messages m;

    public PasspartuCommand(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) {
    	
        if (!plugin.authmePermissible(sender, "authme.admin." + args[0].toLowerCase())) {
            sender.sendMessage(m._("no_perm"));
            return true;
        }

       if (PlayerCache.getInstance().isAuthenticated(sender.getName().toLowerCase())) {
            return true;
        }

       if ((sender instanceof Player) && args.length == 1) {
           if(utils.readToken(args[0])) {
                 //bypass login!
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
