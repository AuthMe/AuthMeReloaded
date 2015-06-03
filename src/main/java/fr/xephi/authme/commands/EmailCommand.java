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
        String name = player.getName();

        if (args.length == 0) {
            m.send(player, "usage_email_add");
            m.send(player, "usage_email_change");
            m.send(player, "usage_email_recovery");
            return true;
        }

        if (args[0].equalsIgnoreCase("add")) {
            if (args.length != 3) {
                m.send(player, "usage_email_add");
                return true;
            }
            if (Settings.getmaxRegPerEmail > 0) {
                if (!plugin.authmePermissible(sender, "authme.allow2accounts") && data.getAllAuthsByEmail(args[1]).size() >= Settings.getmaxRegPerEmail) {
                    m.send(player, "max_reg");
                    return true;
                }
            }
            if (args[1].equals(args[2]) && PlayerCache.getInstance().isAuthenticated(name)) {
                PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
                if (auth.getEmail() == null || (!auth.getEmail().equals("your@email.com") && !auth.getEmail().isEmpty())) {
                    m.send(player, "usage_email_change");
                    return true;
                }
                if (!Settings.isEmailCorrect(args[1])) {
                    m.send(player, "email_invalid");
                    return true;
                }
                auth.setEmail(args[1]);
                if (!data.updateEmail(auth)) {
                    m.send(player, "error");
                    return true;
                }
                PlayerCache.getInstance().updatePlayer(auth);
                m.send(player, "email_added");
                player.sendMessage(auth.getEmail());
            } else if (PlayerCache.getInstance().isAuthenticated(name)) {
                m.send(player, "email_confirm");
            } else {
                if (!data.isAuthAvailable(name)) {
                    m.send(player, "login_msg");
                } else {
                    m.send(player, "reg_email_msg");
                }
            }
        } else if (args[0].equalsIgnoreCase("change")) {
            if (args.length != 3) {
                m.send(player, "usage_email_change");
                return true;
            }
            if (Settings.getmaxRegPerEmail > 0) {
                if (!plugin.authmePermissible(sender, "authme.allow2accounts") && data.getAllAuthsByEmail(args[2]).size() >= Settings.getmaxRegPerEmail) {
                    m.send(player, "max_reg");
                    return true;
                }
            }
            if (PlayerCache.getInstance().isAuthenticated(name)) {
                PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
                if (auth.getEmail() == null || auth.getEmail().equals("your@email.com") || auth.getEmail().isEmpty()) {
                    m.send(player, "usage_email_add");
                    return true;
                }
                if (!args[1].equals(auth.getEmail())) {
                    m.send(player, "old_email_invalid");
                    return true;
                }
                if (!Settings.isEmailCorrect(args[2])) {
                    m.send(player, "new_email_invalid");
                    return true;
                }
                auth.setEmail(args[2]);
                if (!data.updateEmail(auth)) {
                    m.send(player, "error");
                    return true;
                }
                PlayerCache.getInstance().updatePlayer(auth);
                m.send(player, "email_changed");
                player.sendMessage(m.send("email_defined") + auth.getEmail());
            } else if (PlayerCache.getInstance().isAuthenticated(name)) {
                m.send(player, "email_confirm");
            } else {
                if (!data.isAuthAvailable(name)) {
                    m.send(player, "login_msg");
                } else {
                    m.send(player, "reg_email_msg");
                }
            }
        }
        if (args[0].equalsIgnoreCase("recovery")) {
            if (args.length != 2) {
                m.send(player, "usage_email_recovery");
                return true;
            }
            if (plugin.mail == null) {
                m.send(player, "error");
                return true;
            }
            if (data.isAuthAvailable(name)) {
                if (PlayerCache.getInstance().isAuthenticated(name)) {
                    m.send(player, "logged_in");
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
                        m.send(player, "unknown_user");
                        return true;
                    }
                    if (Settings.getmailAccount.equals("") || Settings.getmailAccount.isEmpty()) {
                        m.send(player, "error");
                        return true;
                    }

                    if (!args[1].equalsIgnoreCase(auth.getEmail()) || args[1].equalsIgnoreCase("your@email.com") || auth.getEmail().equalsIgnoreCase("your@email.com")) {
                        m.send(player, "email_invalid");
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
                    m.send(player, "email_send");
                } catch (NoSuchAlgorithmException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    m.send(sender, "error");
                } catch (NoClassDefFoundError ncdfe) {
                    ConsoleLogger.showError(ncdfe.getMessage());
                    m.send(sender, "error");
                }
            } else {
                m.send(player, "reg_email_msg");
            }
        }
        return true;
    }
}
