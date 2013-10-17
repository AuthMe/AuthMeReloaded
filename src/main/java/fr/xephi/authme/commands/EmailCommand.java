package fr.xephi.authme.commands;

import java.security.NoSuchAlgorithmException;

import org.bukkit.Bukkit;
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
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

/**
 *
 * @author Xephi59
 */
public class EmailCommand implements CommandExecutor {

	public AuthMe plugin;
	private DataSource data;
    private Messages m = Messages.getInstance();

    public EmailCommand(AuthMe plugin, DataSource data) {
        this.plugin = plugin;
        this.data = data;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }  

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            sender.sendMessage(m._("no_perm"));
            return true;
        }

       Player player = (Player) sender;
       String name = player.getName().toLowerCase();

       if (args.length == 0) {
           player.sendMessage(m._("usage_email_add"));
           player.sendMessage(m._("usage_email_change"));
           player.sendMessage(m._("usage_email_recovery"));
           return true;
       }

        if(args[0].equalsIgnoreCase("add")) {
        	if (args.length != 3) {
        		player.sendMessage(m._("usage_email_add"));
        		return true;
        	}
            if(args[1].equals(args[2]) && PlayerCache.getInstance().isAuthenticated(name)) {
                PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
                if (auth.getEmail() == null || (!auth.getEmail().equals("your@email.com") && !auth.getEmail().isEmpty())) {
                	player.sendMessage("usage_email_change");
                	return true;
                }
                if (!args[1].contains("@")) {
                	player.sendMessage(m._("email_invalid"));
                	return true;
                }
                auth.setEmail(args[1]);
                if (!data.updateEmail(auth)) {
                    player.sendMessage(m._("error"));
                    return true;
                }
                PlayerCache.getInstance().updatePlayer(auth);
                player.sendMessage(m._("email_added"));
                player.sendMessage(auth.getEmail());
            } else if (PlayerCache.getInstance().isAuthenticated(name)){
                player.sendMessage(m._("email_confirm"));
            } else {
            	if (!data.isAuthAvailable(name)) {
            		player.sendMessage(m._("login_msg"));
            	} else {
            		player.sendMessage(m._("reg_email_msg"));
            	}
            }
        } else if(args[0].equalsIgnoreCase("change") && args.length == 3 ) {
            if(PlayerCache.getInstance().isAuthenticated(name)) {
                PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
                if (auth.getEmail() == null || auth.getEmail().equals("your@email.com") || auth.getEmail().isEmpty()) {
                	player.sendMessage(m._("usage_email_add"));
                	return true;
                }
                if (!args[1].equals(auth.getEmail())) { 
                	player.sendMessage(m._("old_email_invalid"));
                	return true;
                }
                if (!args[2].contains("@")) {
                	player.sendMessage(m._("new_email_invalid"));
                	return true;
                }
                auth.setEmail(args[2]);
                if (!data.updateEmail(auth)) {
                    player.sendMessage(m._("bad_database_email"));
                    return true;
                }
                PlayerCache.getInstance().updatePlayer(auth);
                player.sendMessage(m._("email_changed"));
                player.sendMessage(m._("email_defined") + auth.getEmail());
            } else if (PlayerCache.getInstance().isAuthenticated(name)){
                player.sendMessage(m._("email_confirm"));
            } else {
            	if (!data.isAuthAvailable(name)) {
            		player.sendMessage(m._("login_msg"));
            	} else {
            		player.sendMessage(m._("reg_email_msg"));
            	}
            }
        }
        if(args[0].equalsIgnoreCase("recovery")) {
        	if (args.length != 2) {
        		player.sendMessage(m._("usage_email_recovery"));
        		return true;
        	}
        	if (plugin.mail == null) {
        		player.sendMessage(m._("error"));
        		return true;
        	}
        	if (data.isAuthAvailable(name)) {
        		if (PlayerCache.getInstance().isAuthenticated(name)) {
        			player.sendMessage(m._("logged_in"));
        			return true;
        		}
        			try {
            			RandomString rand = new RandomString(Settings.getRecoveryPassLength);
            			String thePass = rand.nextString();
						String hashnew = PasswordSecurity.getHash(Settings.getPasswordHash, thePass, name);
		                PlayerAuth auth = null;
		                if (PlayerCache.getInstance().isAuthenticated(name)) {
		                    auth = PlayerCache.getInstance().getAuth(name);
		                } else if (data.isAuthAvailable(name)) {
		                    auth = data.getAuth(name);
		                } else {
		                    sender.sendMessage(m._("unknown_user"));
		                    return true;
		                }
		        		if (Settings.getmailAccount.equals("") || Settings.getmailAccount.isEmpty()) {
		        			player.sendMessage(m._("error"));
		        			return true;
		        		}
		        		
		        		if (!args[1].equalsIgnoreCase(auth.getEmail())) { 
		        			player.sendMessage(m._("email_invalid"));
		        			return true;
		        		}
		        		final String finalhashnew = hashnew;
		        		final PlayerAuth finalauth = auth;
		        		if (data instanceof Thread) {
		        			finalauth.setHash(hashnew);
		        			data.updatePassword(auth);
		        		} else {
			        		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
								@Override
								public void run() {
									finalauth.setHash(finalhashnew);
									data.updatePassword(finalauth);
								}
			        		});
		        		}
		                plugin.mail.main(auth, thePass);
		                player.sendMessage(m._("email_send"));
					} catch (NoSuchAlgorithmException ex) {
			            ConsoleLogger.showError(ex.getMessage());
			            sender.sendMessage(m._("error"));
					} catch (NoClassDefFoundError ncdfe) {
						ConsoleLogger.showError(ncdfe.getMessage());
			            sender.sendMessage(m._("error"));
					}
        		} else {
        		player.sendMessage(m._("reg_email_msg"));
        	}
        }
        return true;
    }
}
