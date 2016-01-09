package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * Admin command to register a user.
 */
public class RegisterAdminCommand implements ExecutableCommand {

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments,
                               final CommandService commandService) {
        // Get the player name and password
        final String playerName = arguments.get(0).toLowerCase();
        final String playerPass = arguments.get(1).toLowerCase();
        final String playerNameLowerCase = playerName.toLowerCase();
        final String playerPassLowerCase = playerPass.toLowerCase();

        // Command logic
        if (!playerPassLowerCase.matches(Settings.getPassRegex)) {
            commandService.send(sender, MessageKey.PASSWORD_MATCH_ERROR);
            return;
        }
        if (playerPassLowerCase.equalsIgnoreCase(playerName)) {
            commandService.send(sender, MessageKey.PASSWORD_IS_USERNAME_ERROR);
            return;
        }
        if (playerPassLowerCase.length() < Settings.getPasswordMinLen
            || playerPassLowerCase.length() > Settings.passwordMaxLength) {
            commandService.send(sender, MessageKey.INVALID_PASSWORD_LENGTH);
            return;
        }
        if (!Settings.unsafePasswords.isEmpty() && Settings.unsafePasswords.contains(playerPassLowerCase)) {
            commandService.send(sender, MessageKey.PASSWORD_UNSAFE_ERROR);
            return;
        }
        commandService.runTaskAsynchronously(new Runnable() {

            @Override
            public void run() {
                if (commandService.getDataSource().isAuthAvailable(playerNameLowerCase)) {
                    commandService.send(sender, MessageKey.NAME_ALREADY_REGISTERED);
                    return;
                }
                HashedPassword hashedPassword = commandService.getPasswordSecurity()
                    .computeHash(playerPass, playerNameLowerCase);
                PlayerAuth auth = PlayerAuth.builder()
                    .name(playerNameLowerCase)
                    .realName(playerName)
                    .password(hashedPassword)
                    .build();

                if (!commandService.getDataSource().saveAuth(auth)) {
                    commandService.send(sender, MessageKey.ERROR);
                    return;
                }
                commandService.getDataSource().setUnlogged(playerNameLowerCase);
                if (Bukkit.getPlayerExact(playerName) != null) {
                    Bukkit.getPlayerExact(playerName).kickPlayer("An admin just registered you, please log again");
                } else {
                    commandService.send(sender, MessageKey.REGISTER_SUCCESS);
                    ConsoleLogger.info(playerName + " registered");
                }
            }
        });
    }
}
