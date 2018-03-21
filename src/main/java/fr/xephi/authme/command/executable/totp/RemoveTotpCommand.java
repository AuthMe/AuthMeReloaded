package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.totp.TotpService;
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
    private TotpService totpService;

    @Inject
    private Messages messages;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        PlayerAuth auth = dataSource.getAuth(player.getName());
        if (auth.getTotpKey() == null) {
            messages.send(player, MessageKey.TWO_FACTOR_NOT_ENABLED_ERROR);
        } else {
            if (totpService.verifyCode(auth, arguments.get(0))) {
                dataSource.removeTotpKey(auth.getNickname());
                messages.send(player, MessageKey.TWO_FACTOR_REMOVED_SUCCESS);
            } else {
                messages.send(player, MessageKey.TWO_FACTOR_INVALID_CODE);
            }
        }
    }
}
