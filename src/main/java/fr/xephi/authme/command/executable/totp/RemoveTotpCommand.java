package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.totp.TotpAuthenticator;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for a player to remove 2FA authentication.
 */
public class RemoveTotpCommand extends PlayerCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private TotpAuthenticator totpAuthenticator;

    @Inject
    private Messages messages;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        PlayerAuth auth = playerCache.getAuth(player.getName());
        if (auth == null) {
            messages.send(player, MessageKey.NOT_LOGGED_IN);
        } else if (auth.getTotpKey() == null) {
            messages.send(player, MessageKey.TWO_FACTOR_NOT_ENABLED_ERROR);
        } else {
            if (totpAuthenticator.checkCode(auth, arguments.get(0))) {
                removeTotpKeyFromDatabase(player, auth);
            } else {
                messages.send(player, MessageKey.TWO_FACTOR_INVALID_CODE);
            }
        }
    }

    private void removeTotpKeyFromDatabase(Player player, PlayerAuth auth) {
        if (dataSource.removeTotpKey(auth.getNickname())) {
            auth.setTotpKey(null);
            playerCache.updatePlayer(auth);
            messages.send(player, MessageKey.TWO_FACTOR_REMOVED_SUCCESS);
            ConsoleLogger.info("Player '" + player.getName() + "' removed their TOTP key");
        } else {
            messages.send(player, MessageKey.ERROR);
        }
    }
}
