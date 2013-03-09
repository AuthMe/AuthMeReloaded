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
import uk.org.whoami.authme.Management;
import uk.org.whoami.authme.Utils;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.datasource.DataSource;

/**
 *
 * @author stefano
 */
public class PasspartuCommand implements CommandExecutor {
    private Utils utils = new Utils();
    private DataSource database;
    public AuthMe plugin;
    
    public PasspartuCommand(DataSource database, AuthMe plugin) {
        this.database = database;
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) { 
        
       if (PlayerCache.getInstance().isAuthenticated(sender.getName().toLowerCase())) {
            return true;
        }
        
       if ((sender instanceof Player) && args.length == 1) {
           if(utils.readToken(args[0])) {
                 //bypass login!
                Management bypass = new Management(database,true, plugin);
                String result = bypass.performLogin((Player)sender, "dontneed");
                if (result != "") sender.sendMessage(result); 
                    return true;
           }
           
           sender.sendMessage("Time is expired or Token is Wrong!");
           return true;
       }
       sender.sendMessage("usage: /passpartu token");
       return true;
    }
}
