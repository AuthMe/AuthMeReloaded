package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.data.CaptchaManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.message.MessageKey;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

public class CaptchaCommand extends PlayerCommand {

    @Inject
    private PlayerCache playerCache;

    @Inject
    private CaptchaManager captchaManager;

    @Inject
    private CommandService commandService;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        final String playerName = player.getName().toLowerCase();

        if (playerCache.isAuthenticated(playerName)) {
            commandService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
        } else if (!captchaManager.isCaptchaRequired(playerName)) {
            commandService.send(player, MessageKey.USAGE_LOGIN);
        } else {
            checkCaptcha(player, arguments.get(0));
        }
    }

    private void checkCaptcha(Player player, String captchaCode) {
        final boolean isCorrectCode = captchaManager.checkCode(player.getName(), captchaCode);
        if (isCorrectCode) {
            commandService.send(player, MessageKey.CAPTCHA_SUCCESS);
            commandService.send(player, MessageKey.LOGIN_MESSAGE);
        } else {
            String newCode = captchaManager.generateCode(player.getName());
            commandService.send(player, MessageKey.CAPTCHA_WRONG_ERROR, newCode);
        }
    }
}
