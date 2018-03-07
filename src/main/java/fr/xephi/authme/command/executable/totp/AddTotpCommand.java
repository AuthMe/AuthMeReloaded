package fr.xephi.authme.command.executable.totp;

import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.TotpService;
import fr.xephi.authme.security.TotpService.TotpGenerationResult;
import fr.xephi.authme.service.CommonService;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Command for a player to enable TOTP.
 */
public class AddTotpCommand extends PlayerCommand {

    @Inject
    private TotpService totpService;

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Override
    protected void runCommand(Player player, List<String> arguments) {
        PlayerAuth auth = dataSource.getAuth(player.getName());
        if (auth.getTotpKey() == null) {
            TotpGenerationResult createdTotpInfo = totpService.generateTotpKey(player);
            commonService.send(player, MessageKey.TWO_FACTOR_CREATE,
                createdTotpInfo.getTotpKey(), createdTotpInfo.getAuthenticatorQrCodeUrl());
        } else {
            player.sendMessage(ChatColor.RED + "Two-factor authentication is already enabled for your account!");
        }
    }
}
