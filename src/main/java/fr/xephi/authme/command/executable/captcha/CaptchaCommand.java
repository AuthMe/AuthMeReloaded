package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CaptchaCommand extends ExecutableCommand {

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

        // Random string instance, for captcha generation (I think) -- timvisee
        RandomString randStr = new RandomString(Settings.captchaLength);

        // Get the parameter values
        String captcha = commandArguments.get(0);

        // Make sure the current command executor is a player
        if(!(sender instanceof Player)) {
            return true;
        }

        // Get the player instance and name
        final Player player = (Player) sender;
        final String playerNameLowerCase = player.getName().toLowerCase();

        // Command logic
        if (PlayerCache.getInstance().isAuthenticated(playerNameLowerCase)) {
            m.send(player, "logged_in");
            return true;
        }

        if (!Settings.useCaptcha) {
            m.send(player, "usage_log");
            return true;
        }

        if (!plugin.cap.containsKey(playerNameLowerCase)) {
            m.send(player, "usage_log");
            return true;
        }

        if (Settings.useCaptcha && !captcha.equals(plugin.cap.get(playerNameLowerCase))) {
            plugin.cap.remove(playerNameLowerCase);
            plugin.cap.put(playerNameLowerCase, randStr.nextString());
            for (String s : m.send("wrong_captcha")) {
                player.sendMessage(s.replace("THE_CAPTCHA", plugin.cap.get(playerNameLowerCase)));
            }
            return true;
        }

        try {
            plugin.captcha.remove(playerNameLowerCase);
            plugin.cap.remove(playerNameLowerCase);
        } catch (NullPointerException ignored) { }

        // Show a status message
        m.send(player, "valid_captcha");
        m.send(player, "login_msg");
        return true;
    }
}
