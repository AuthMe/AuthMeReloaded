package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.TotpService;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command to enable TOTP by supplying the proper code as confirmation.
 */
public class ConfirmTotpCommand extends PlayerCommand {

    @Inject
    private TotpService totpService;

    @Inject
    private DataSource dataSource;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        // TODO #1141: Check if player already has TOTP

        final String totpKey = totpService.retrieveGeneratedSecret(player);
        if (totpKey == null) {
            player.sendMessage("No TOTP key has been generated for you or it has expired. Please run /totp add");
        } else {
            boolean isTotpCodeValid = totpService.confirmCodeForGeneratedTotpKey(player, arguments.get(0));
            if (isTotpCodeValid) {
                dataSource.setTotpKey(player.getName(), totpKey);
                player.sendMessage("Successfully enabled two-factor authentication for your account");
            } else {
                player.sendMessage("Wrong code or code has expired. Please use /totp add again");
            }
        }
    }
}
