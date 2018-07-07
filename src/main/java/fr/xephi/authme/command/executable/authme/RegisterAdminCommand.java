package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.ValidationService.ValidationResult;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Admin command to register a user.
 */
public class RegisterAdminCommand implements ExecutableCommand {

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private CommonService commonService;

    @Inject
    private DataSource dataSource;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ValidationService validationService;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // Get the player name and password
        final String playerName = arguments.get(0);
        final String playerPass = arguments.get(1);
        final String playerNameLowerCase = playerName.toLowerCase();

        // Command logic
        ValidationResult passwordValidation = validationService.validatePassword(playerPass, playerName);
        if (passwordValidation.hasError()) {
            commonService.send(sender, passwordValidation.getMessageKey(), passwordValidation.getArgs());
            return;
        }

        bukkitService.runTaskOptionallyAsync(() -> {
            if (dataSource.isAuthAvailable(playerNameLowerCase)) {
                commonService.send(sender, MessageKey.NAME_ALREADY_REGISTERED);
                return;
            }
            HashedPassword hashedPassword = passwordSecurity.computeHash(playerPass, playerNameLowerCase);
            PlayerAuth auth = PlayerAuth.builder()
                .name(playerNameLowerCase)
                .realName(playerName)
                .password(hashedPassword)
                .registrationDate(System.currentTimeMillis())
                .build();

            if (!dataSource.saveAuth(auth)) {
                commonService.send(sender, MessageKey.ERROR);
                return;
            }

            commonService.send(sender, MessageKey.REGISTER_SUCCESS);
            ConsoleLogger.info(sender.getName() + " registered " + playerName);
            final Player player = bukkitService.getPlayerExact(playerName);
            if (player != null) {
                bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() ->
                    player.kickPlayer(commonService.retrieveSingleMessage(player, MessageKey.KICK_FOR_ADMIN_REGISTER)));
            }
        });
    }
}
