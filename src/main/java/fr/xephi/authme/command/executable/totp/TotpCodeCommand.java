package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboPlayerState;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.security.totp.TotpAuthenticator;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * TOTP code command for processing the 2FA code during the login process.
 */
public class TotpCodeCommand extends PlayerCommand {

    @Inject
    private LimboService limboService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private Messages messages;

    @Inject
    private TotpAuthenticator totpAuthenticator;

    @Inject
    private DataSource dataSource;

    @Inject
    private AsynchronousLogin asynchronousLogin;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        if (playerCache.isAuthenticated(player.getName())) {
            messages.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return;
        }

        PlayerAuth auth = dataSource.getAuth(player.getName());
        if (auth == null) {
            messages.send(player, MessageKey.REGISTER_MESSAGE);
            return;
        }

        LimboPlayer limbo = limboService.getLimboPlayer(player.getName());
        if (limbo != null && limbo.getState() == LimboPlayerState.TOTP_REQUIRED) {
            processCode(player, auth, arguments.get(0));
        } else {
            ConsoleLogger.debug(() -> "Aborting TOTP check for player '" + player.getName()
                + "'. Invalid limbo state: " + (limbo == null ? "no limbo" : limbo.getState()));
            messages.send(player, MessageKey.LOGIN_MESSAGE);
        }
    }

    private void processCode(Player player, PlayerAuth auth, String inputCode) {
        boolean isCodeValid = totpAuthenticator.checkCode(auth, inputCode);
        if (isCodeValid) {
            ConsoleLogger.debug("Successfully checked TOTP code for `{0}`", player.getName());
            asynchronousLogin.performLogin(player, auth);
        } else {
            ConsoleLogger.debug("Input TOTP code was invalid for player `{0}`", player.getName());
            messages.send(player, MessageKey.TWO_FACTOR_INVALID_CODE);
        }
    }
}
