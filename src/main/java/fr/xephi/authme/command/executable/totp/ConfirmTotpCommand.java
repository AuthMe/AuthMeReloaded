package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.totp.GenerateTotpService;
import fr.xephi.authme.security.totp.TotpAuthenticator.TotpGenerationResult;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command to enable TOTP by supplying the proper code as confirmation.
 */
public class ConfirmTotpCommand extends PlayerCommand {

    @Inject
    private GenerateTotpService generateTotpService;

    @Inject
    private DataSource dataSource;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        // TODO #1141: Check if player already has TOTP

        final TotpGenerationResult totpDetails = generateTotpService.getGeneratedTotpKey(player);
        if (totpDetails == null) {
            player.sendMessage("No TOTP key has been generated for you or it has expired. Please run /totp add");
        } else {
            boolean isCodeValid = generateTotpService.isTotpCodeCorrectForGeneratedTotpKey(player, arguments.get(0));
            if (isCodeValid) {
                generateTotpService.removeGenerateTotpKey(player);
                dataSource.setTotpKey(player.getName(), totpDetails.getTotpKey());
                player.sendMessage("Successfully enabled two-factor authentication for your account");
            } else {
                player.sendMessage("Wrong code or code has expired. Please use /totp add again");
            }
        }
    }
}
