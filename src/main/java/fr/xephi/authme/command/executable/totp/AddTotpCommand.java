package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.security.totp.GenerateTotpService;
import fr.xephi.authme.security.totp.TotpAuthenticator.TotpGenerationResult;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for a player to enable TOTP.
 */
public class AddTotpCommand extends PlayerCommand {

    @Inject
    private GenerateTotpService generateTotpService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private Messages messages;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        PlayerAuth auth = playerCache.getAuth(player.getName());
        if (auth == null) {
            messages.send(player, MessageKey.NOT_LOGGED_IN);
        } else if (auth.getTotpKey() == null) {
            TotpGenerationResult createdTotpInfo = generateTotpService.generateTotpKey(player);
            messages.send(player, MessageKey.TWO_FACTOR_CREATE,
                createdTotpInfo.getTotpKey(), createdTotpInfo.getAuthenticatorQrCodeUrl());
            messages.send(player, MessageKey.TWO_FACTOR_CREATE_CONFIRMATION_REQUIRED);
        } else {
            messages.send(player, MessageKey.TWO_FACTOR_ALREADY_ENABLED);
        }
    }
}
