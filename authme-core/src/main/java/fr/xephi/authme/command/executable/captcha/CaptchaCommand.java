package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.captcha.LoginCaptchaManager;
import fr.xephi.authme.data.captcha.RegistrationCaptchaManager;
import fr.xephi.authme.data.limbo.LimboMessageType;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Captcha command, allowing a player to solve a captcha.
 */
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

    @Inject
    private DataSource dataSource;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        final String name = player.getName();

        if (playerCache.isAuthenticated(name)) {
            // No captcha is relevant if the player is logged in
            commonService.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return;
        }

        if (loginCaptchaManager.isCaptchaRequired(name)) {
            checkLoginCaptcha(player, arguments.get(0));
        } else {
            final boolean isPlayerRegistered = dataSource.isAuthAvailable(name);
            if (!isPlayerRegistered && registrationCaptchaManager.isCaptchaRequired(name)) {
                checkRegisterCaptcha(player, arguments.get(0));
            } else {
                MessageKey errorMessage = isPlayerRegistered ? MessageKey.USAGE_LOGIN : MessageKey.USAGE_REGISTER;
                commonService.send(player, errorMessage);
            }
        }
    }

    private void checkLoginCaptcha(Player player, String captchaCode) {
        final boolean isCorrectCode = loginCaptchaManager.checkCode(player, captchaCode);
        if (isCorrectCode) {
            commonService.send(player, MessageKey.CAPTCHA_SUCCESS);
            commonService.send(player, MessageKey.LOGIN_MESSAGE);
            limboService.unmuteMessageTask(player);
        } else {
            String newCode = loginCaptchaManager.getCaptchaCodeOrGenerateNew(player.getName());
            commonService.send(player, MessageKey.CAPTCHA_WRONG_ERROR, newCode);
        }
    }

    private void checkRegisterCaptcha(Player player, String captchaCode) {
        final boolean isCorrectCode = registrationCaptchaManager.checkCode(player, captchaCode);
        if (isCorrectCode) {
            commonService.send(player, MessageKey.REGISTER_CAPTCHA_SUCCESS);
            commonService.send(player, MessageKey.REGISTER_MESSAGE);
        } else {
            String newCode = registrationCaptchaManager.getCaptchaCodeOrGenerateNew(player.getName());
            commonService.send(player, MessageKey.CAPTCHA_WRONG_ERROR, newCode);
        }
        limboService.resetMessageTask(player, LimboMessageType.REGISTER);
    }
}
