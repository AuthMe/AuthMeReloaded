package fr.xephi.authme.process.email;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PendingEmailVerificationCache;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Locale;

/**
 * Async task that validates an email confirmation code and persists the pending address.
 */
public class AsyncConfirmEmail implements AsynchronousProcess {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(AsyncConfirmEmail.class);

    @Inject
    private CommonService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private DataSource dataSource;

    @Inject
    private PendingEmailVerificationCache pendingEmailVerificationCache;

    AsyncConfirmEmail() {
    }

    /**
     * Validates the supplied code against the pending email confirmation for the player.
     * On success, saves the new address to the database and player cache.
     *
     * @param player the player submitting the code
     * @param code   the confirmation code
     */
    public void confirmEmail(Player player, String code) {
        String playerName = player.getName().toLowerCase(Locale.ROOT);

        if (!playerCache.isAuthenticated(playerName)) {
            if (dataSource.isAuthAvailable(player.getName())) {
                service.send(player, MessageKey.LOGIN_MESSAGE);
            } else {
                service.send(player, MessageKey.REGISTER_MESSAGE);
            }
            return;
        }

        PendingEmailVerificationCache.PendingEntry entry =
            pendingEmailVerificationCache.getEntry(player.getName());
        if (entry == null) {
            service.send(player, MessageKey.EMAIL_CONFIRM_CODE_EXPIRED);
            return;
        }

        if (!entry.code().equals(code)) {
            service.send(player, MessageKey.EMAIL_CONFIRM_WRONG_CODE);
            return;
        }

        pendingEmailVerificationCache.removePending(player.getName());

        PlayerAuth auth = playerCache.getAuth(playerName);
        auth.setEmail(entry.email());
        if (dataSource.updateEmail(auth)) {
            playerCache.updatePlayer(auth);
            service.send(player, MessageKey.EMAIL_CONFIRM_SUCCESS);
        } else {
            logger.warning("Could not save confirmed email for player '" + player + "'");
            service.send(player, MessageKey.ERROR);
        }
    }
}
