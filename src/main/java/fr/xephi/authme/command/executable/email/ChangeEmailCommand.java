package fr.xephi.authme.command.executable.email;

import java.util.Arrays;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

public class ChangeEmailCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = Messages.getInstance();

        // Get the parameter values
        String playerMailOld = commandArguments.get(0);
        String playerMailNew = commandArguments.get(1);

        // Make sure the current command executor is a player
        if(!(sender instanceof Player)) {
            return true;
        }

        // Get the player instance and name
        final Player player = (Player) sender;
        final String playerName = player.getName();

        // Command logic
        if (Settings.getmaxRegPerEmail > 0) {
            if (!plugin.authmePermissible(sender, "authme.allow2accounts") && plugin.database.getAllAuthsByEmail(playerMailNew).size() >= Settings.getmaxRegPerEmail) {
                m.send(player, "max_reg");
                return true;
            }
        }
        if (PlayerCache.getInstance().isAuthenticated(playerName)) {
            PlayerAuth auth = PlayerCache.getInstance().getAuth(playerName);
            if (auth.getEmail() == null || auth.getEmail().equals("your@email.com") || auth.getEmail().isEmpty()) {
                m.send(player, "usage_email_add");
                return true;
            }
            if (!playerMailOld.equals(auth.getEmail())) {
                m.send(player, "old_email_invalid");
                return true;
            }
            if (!Settings.isEmailCorrect(playerMailNew)) {
                m.send(player, "new_email_invalid");
                return true;
            }
            auth.setEmail(playerMailNew);
            if (!plugin.database.updateEmail(auth)) {
                m.send(player, "error");
                return true;
            }
            PlayerCache.getInstance().updatePlayer(auth);
            m.send(player, "email_changed");
            player.sendMessage(Arrays.toString(m.send("email_defined")) + auth.getEmail());
        } else if (PlayerCache.getInstance().isAuthenticated(playerName)) {
            m.send(player, "email_confirm");
        } else {
            if (!plugin.database.isAuthAvailable(playerName)) {
                m.send(player, "login_msg");
            } else {
                m.send(player, "reg_email_msg");
            }
        }

        return true;
    }
}
