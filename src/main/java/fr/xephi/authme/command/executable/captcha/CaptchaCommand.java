package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 */
public class CaptchaCommand extends ExecutableCommand {

    @Override
    public boolean executeCommand(CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return true;
        }

        // Get the player instance and name
        final Player player = (Player) sender;
        final String playerNameLowerCase = player.getName().toLowerCase();

        // Get the parameter values
        String captcha = commandArguments.get(0);

        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = plugin.getMessages();

        // Command logic
        if (PlayerCache.getInstance().isAuthenticated(playerNameLowerCase)) {
            m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return true;
        }

        if (!Settings.useCaptcha) {
            m.send(player, MessageKey.USAGE_LOGIN);
            return true;
        }


        if (!plugin.cap.containsKey(playerNameLowerCase)) {
            m.send(player, MessageKey.USAGE_LOGIN);
            return true;
        }

        if (Settings.useCaptcha && !captcha.equals(plugin.cap.get(playerNameLowerCase))) {
            plugin.cap.remove(playerNameLowerCase);
            String randStr = new RandomString(Settings.captchaLength).nextString();
            plugin.cap.put(playerNameLowerCase, randStr);
            for (String s : m.retrieve(MessageKey.CAPTCHA_WRONG_ERROR)) {
                player.sendMessage(s.replace("THE_CAPTCHA", plugin.cap.get(playerNameLowerCase)));
            }
            return true;
        }

        plugin.captcha.remove(playerNameLowerCase);
        plugin.cap.remove(playerNameLowerCase);

        // Show a status message
        m.send(player, MessageKey.CAPTCHA_SUCCESS);
        m.send(player, MessageKey.LOGIN_MESSAGE);
        return true;
    }
}
