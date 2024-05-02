package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsyncUserScheduler;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Admin command for setting an email to an account.
 */
public class SetEmailCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ValidationService validationService;

    @Inject
    private AsyncUserScheduler asyncUserScheduler;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // Get the player name and email address
        String playerName = arguments.get(0);
        String playerEmail = arguments.get(1);

        // Validate the email address
        if (!validationService.validateEmail(playerEmail)) {
            commonService.send(sender, MessageKey.INVALID_EMAIL);
            return;
        }

        String lowercasePlayerName = playerName.toLowerCase();
        asyncUserScheduler.runTask(lowercasePlayerName, (Runnable) () -> {
            // Validate the user
            PlayerAuth auth = dataSource.getAuth(playerName);
            if (auth == null) {
                commonService.send(sender, MessageKey.UNKNOWN_USER);
                return;
            } else if (!validationService.isEmailFreeForRegistration(playerEmail, sender)) {
                commonService.send(sender, MessageKey.EMAIL_ALREADY_USED_ERROR);
                return;
            }

            // Set the email address
            auth.setEmail(playerEmail);
            if (!dataSource.updateEmail(auth)) {
                commonService.send(sender, MessageKey.ERROR);
                return;
            }

            // Update the player cache
            if (playerCache.getAuth(playerName) != null) {
                playerCache.updatePlayer(auth);
            }

            // Show a status message
            commonService.send(sender, MessageKey.EMAIL_CHANGED_SUCCESS);
        });
    }
}
