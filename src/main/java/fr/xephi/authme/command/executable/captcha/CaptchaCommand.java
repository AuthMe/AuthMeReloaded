package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.LoginCaptchaManager;
import fr.xephi.authme.data.RegistrationCaptchaManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

public class CaptchaCommand extends PlayerCommand {

    @Inject
    private PlayerCache playerCache;

    @Inject
    private LoginCaptchaManager loginCaptchaManager;

    @Inject
    private RegistrationCaptchaManager registrationCaptchaManager;

    @Inject
    private CommonService commonService;

    @Inject
    private LimboService limboService;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        final String name = player.getName();

        if (playerCache.isAuthenticated(name)) {
            commonService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
        } else if (loginCaptchaManager.isCaptchaRequired(name)) {
            checkLoginCaptcha(player, arguments.get(0));
        } else if (registrationCaptchaManager.isCaptchaRequired(name)) {
            checkRegisterCaptcha(player, arguments.get(0));
        } else {
            MessageKey errorMessage = playerCache.isAuthenticated(name)
                ? MessageKey.ALREADY_LOGGED_IN_ERROR : MessageKey.USAGE_LOGIN;
            commonService.send(player, errorMessage);
        }
    }

    private void checkLoginCaptcha(Player player, String captchaCode) {
        final boolean isCorrectCode = loginCaptchaManager.checkCode(player.getName(), captchaCode);
        if (isCorrectCode) {
            commonService.send(player, MessageKey.CAPTCHA_SUCCESS);
            commonService.send(player, MessageKey.LOGIN_MESSAGE);
            limboService.unmuteMessageTask(player);
        } else {
            String newCode = loginCaptchaManager.generateCode(player.getName());
            commonService.send(player, MessageKey.CAPTCHA_WRONG_ERROR, newCode);
        }
    }

    private void checkRegisterCaptcha(Player player, String captchaCode) {
        final boolean isCorrectCode = registrationCaptchaManager.checkCode(player.getName(), captchaCode);
        if (isCorrectCode) {
            commonService.send(player, MessageKey.CAPTCHA_SUCCESS);
            commonService.send(player, MessageKey.REGISTER_MESSAGE);
        } else {
            String newCode = registrationCaptchaManager.generateCode(player.getName());
            commonService.send(player, MessageKey.CAPTCHA_WRONG_ERROR, newCode);
        }
    }
}
