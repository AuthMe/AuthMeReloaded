package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.custom.SecuritySettings;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.entity.Player;

import java.util.List;

public class CaptchaCommand extends PlayerCommand {

    @Override
    public void runCommand(Player player, List<String> arguments, CommandService commandService) {
        final String playerNameLowerCase = player.getName().toLowerCase();
        final String captcha = arguments.get(0);
        Wrapper wrapper = Wrapper.getInstance();
        final AuthMe plugin = wrapper.getAuthMe();
        PlayerCache playerCache = wrapper.getPlayerCache();

        // Command logic
        if (playerCache.isAuthenticated(playerNameLowerCase)) {
            commandService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return;
        }

        if (!commandService.getProperty(SecuritySettings.USE_CAPTCHA)) {
            commandService.send(player, MessageKey.USAGE_LOGIN);
            return;
        }

        if (!plugin.cap.containsKey(playerNameLowerCase)) {
            commandService.send(player, MessageKey.USAGE_LOGIN);
            return;
        }

        if (!captcha.equals(plugin.cap.get(playerNameLowerCase))) {
            plugin.cap.remove(playerNameLowerCase);
            int captchaLength = commandService.getProperty(SecuritySettings.CAPTCHA_LENGTH);
            String randStr = RandomString.generate(captchaLength);
            plugin.cap.put(playerNameLowerCase, randStr);
            commandService.send(player, MessageKey.CAPTCHA_WRONG_ERROR, plugin.cap.get(playerNameLowerCase));
            return;
        }

        plugin.captcha.remove(playerNameLowerCase);
        plugin.cap.remove(playerNameLowerCase);

        // Show a status message
        commandService.send(player, MessageKey.CAPTCHA_SUCCESS);
        commandService.send(player, MessageKey.LOGIN_MESSAGE);
    }
}
