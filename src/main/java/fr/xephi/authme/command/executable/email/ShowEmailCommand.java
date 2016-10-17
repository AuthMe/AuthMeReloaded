package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.PlayerCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Show email command.
 */
public class ShowEmailCommand extends PlayerCommand {
    @Inject
    private CommandService commandService;

    @Inject
    private PlayerCache playerCache;

    @Override
    public void runCommand(Player player, List<String> arguments) {
        PlayerAuth auth = playerCache.getAuth(player.getName());
        if (auth.getEmail() != null && !auth.getEmail().equalsIgnoreCase("your@email.com")) {
            commandService.send(player, MessageKey.EMAIL_SHOW, auth.getEmail());
        } else {
            commandService.send(player, MessageKey.SHOW_NO_EMAIL);
        }
    }
}
