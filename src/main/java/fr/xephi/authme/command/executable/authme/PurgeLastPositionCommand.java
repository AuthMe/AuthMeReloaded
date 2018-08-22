package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.service.bungeecord.MessageType;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;

/**
 * Removes the stored last position of a user or of all.
 */
public class PurgeLastPositionCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService commonService;

    @Inject
    private BungeeSender bungeeSender;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        if ("*".equals(playerName)) {
            for (PlayerAuth auth : dataSource.getAllAuths()) {
                resetLastPosition(auth);
                dataSource.updateQuitLoc(auth);
                bungeeSender.sendAuthMeBungeecordMessage(MessageType.REFRESH_QUITLOC, playerName);
            }
            sender.sendMessage("All players last position locations are now reset");
        } else {
            // Get the user auth and make sure the user exists
            PlayerAuth auth = dataSource.getAuth(playerName);
            if (auth == null) {
                commonService.send(sender, MessageKey.UNKNOWN_USER);
                return;
            }

            resetLastPosition(auth);
            dataSource.updateQuitLoc(auth);
            bungeeSender.sendAuthMeBungeecordMessage(MessageType.REFRESH_QUITLOC, playerName);
            sender.sendMessage(playerName + "'s last position location is now reset");
        }
    }

    private static void resetLastPosition(PlayerAuth auth) {
        auth.setQuitLocX(0d);
        auth.setQuitLocY(0d);
        auth.setQuitLocZ(0d);
        auth.setWorld("world");
    }
}
