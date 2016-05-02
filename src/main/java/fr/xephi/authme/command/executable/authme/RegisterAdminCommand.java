package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
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
    private DataSource dataSource;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments,
                               final CommandService commandService) {
        // Get the player name and password
        final String playerName = arguments.get(0);
        final String playerPass = arguments.get(1);
        final String playerNameLowerCase = playerName.toLowerCase();

        // Command logic
        MessageKey passwordError = commandService.validatePassword(playerPass, playerName);
        if (passwordError != null) {
            commandService.send(sender, passwordError);
            return;
        }

        commandService.runTaskAsynchronously(new Runnable() {

            @Override
            public void run() {
                if (dataSource.isAuthAvailable(playerNameLowerCase)) {
                    commandService.send(sender, MessageKey.NAME_ALREADY_REGISTERED);
                    return;
                }
                HashedPassword hashedPassword = passwordSecurity.computeHash(playerPass, playerNameLowerCase);
                PlayerAuth auth = PlayerAuth.builder()
                    .name(playerNameLowerCase)
                    .realName(playerName)
                    .password(hashedPassword)
                    .build();

                if (!dataSource.saveAuth(auth)) {
                    commandService.send(sender, MessageKey.ERROR);
                    return;
                }
                dataSource.setUnlogged(playerNameLowerCase);

                commandService.send(sender, MessageKey.REGISTER_SUCCESS);
                ConsoleLogger.info(sender.getName() + " registered " + playerName);
                Player player = commandService.getPlayer(playerName);
                if (player != null) {
                    player.kickPlayer("An admin just registered you, please log in again");
                }
            }
        });
    }
}
