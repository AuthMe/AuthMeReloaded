package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class CaptchaCommand extends ExecutableCommand {

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        // Make sure the current command executor is a player
        if (!(sender instanceof Player)) {
            return;
        }

        // Get the player instance and name
        final Player player = (Player) sender;
        final String playerNameLowerCase = player.getName().toLowerCase();

        // Get the parameter values
        String captcha = arguments.get(0);

        // AuthMe plugin instance
        final Wrapper wrapper = Wrapper.getInstance();
        final AuthMe plugin = wrapper.getAuthMe();

        // Messages instance
        final Messages m = wrapper.getMessages();

        // Command logic
        if (PlayerCache.getInstance().isAuthenticated(playerNameLowerCase)) {
            m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return;
        }

        if (!Settings.useCaptcha) {
            m.send(player, MessageKey.USAGE_LOGIN);
            return;
        }


        if (!plugin.cap.containsKey(playerNameLowerCase)) {
            m.send(player, MessageKey.USAGE_LOGIN);
            return;
        }

        if (Settings.useCaptcha && !captcha.equals(plugin.cap.get(playerNameLowerCase))) {
            plugin.cap.remove(playerNameLowerCase);
            String randStr = new RandomString(Settings.captchaLength).nextString();
            plugin.cap.put(playerNameLowerCase, randStr);
            m.send(player, MessageKey.CAPTCHA_WRONG_ERROR, plugin.cap.get(playerNameLowerCase));
            return;
        }

        plugin.captcha.remove(playerNameLowerCase);
        plugin.cap.remove(playerNameLowerCase);

        // Show a status message
        m.send(player, MessageKey.CAPTCHA_SUCCESS);
        m.send(player, MessageKey.LOGIN_MESSAGE);
    }
}
