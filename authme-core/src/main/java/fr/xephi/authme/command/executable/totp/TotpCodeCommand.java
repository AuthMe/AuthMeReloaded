package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.TempbanManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboPlayerState;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.security.totp.TotpAuthenticator;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

/**
 * TOTP code command for processing the 2FA code during the login process.
 */
public class TotpCodeCommand extends PlayerCommand {

    private static final int MAX_TOTP_ATTEMPTS = 3;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(TotpCodeCommand.class);

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

    @Inject
    private TempbanManager tempbanManager;

    @Inject
    private BukkitService bukkitService;

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
            processCode(player, auth, limbo, arguments.get(0));
        } else {
            logger.debug(() -> "Aborting TOTP check for player '" + player.getName()
                + "'. Invalid limbo state: " + (limbo == null ? "no limbo" : limbo.getState()));
            messages.send(player, MessageKey.LOGIN_MESSAGE);
        }
    }

    private void processCode(Player player, PlayerAuth auth, LimboPlayer limbo, String inputCode) {
        boolean isCodeValid = totpAuthenticator.checkCode(auth, inputCode);
        if (isCodeValid) {
            logger.debug("Successfully checked TOTP code for `{0}`", player.getName());
            asynchronousLogin.performLogin(player, auth);
        } else {
            logger.debug("Input TOTP code was invalid for player `{0}`", player.getName());
            handleWrongCode(player, limbo);
        }
    }

    private void handleWrongCode(Player player, LimboPlayer limbo) {
        String ip = PlayerUtils.getPlayerIp(player);
        tempbanManager.increaseCount(ip, player.getName().toLowerCase(Locale.ROOT));
        if (tempbanManager.shouldTempban(ip)) {
            tempbanManager.tempbanPlayer(player);
        } else if (limbo.increaseFailedTotpAttempts() >= MAX_TOTP_ATTEMPTS) {
            String kickMessage = messages.retrieveSingle(player, MessageKey.TWO_FACTOR_INVALID_CODE);
            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, () -> player.kickPlayer(kickMessage));
        } else {
            messages.send(player, MessageKey.TWO_FACTOR_INVALID_CODE);
        }
    }
}
