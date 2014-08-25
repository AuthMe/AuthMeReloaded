package fr.xephi.authme.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class RegisterCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    public PlayerAuth auth;
    public AuthMe plugin;

    public RegisterCommand(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label,
            String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Player Only! Use 'authme register <playername> <password>' instead");
            return true;
        }
        if (args.length == 0) {
            m._(sender, "usage_reg");
        }
        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            m._(sender, "no_perm");
            return true;
        }
        final Player player = (Player) sender;
        if (Settings.emailRegistration && !Settings.getmailAccount.isEmpty()) {
            if (Settings.doubleEmailCheck) {
                if (args.length < 2) {
                    m._(player, "usage_reg");
                    return true;
                }
                if (!args[0].equals(args[1])) {
                    m._(player, "usage_reg");
                    return true;
                }
            }
            final String email = args[0];
            if (!Settings.isEmailCorrect(email)) {
                m._(player, "email_invalid");
                return true;
            }
            RandomString rand = new RandomString(Settings.getRecoveryPassLength);
            final String thePass = rand.nextString();
            plugin.management.performRegister(player, thePass, email);
            return true;
        }
        if (args.length == 0 || (Settings.getEnablePasswordVerifier && args.length < 2)) {
            m._(player, "usage_reg");
            return true;
        }
        if (args.length > 1 && Settings.getEnablePasswordVerifier)
            if (!args[0].equals(args[1])) {
                m._(player, "password_error");
                return true;
            }
        plugin.management.performRegister(player, args[0], "");
        return true;
    }
}
