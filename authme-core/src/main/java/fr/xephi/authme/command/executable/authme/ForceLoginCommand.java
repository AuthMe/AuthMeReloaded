package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.PreJoinDialogService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;

import static fr.xephi.authme.permission.PlayerPermission.CAN_LOGIN_BE_FORCED;

/**
 * Forces the login of a player, i.e. logs the player in without the need of a (correct) password.
 */
public class ForceLoginCommand implements ExecutableCommand {

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private Management management;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private Messages messages;

    @Inject
    private PreJoinDialogService preJoinDialogService;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        String playerName = arguments.isEmpty() ? sender.getName() : arguments.get(0);

        Player player = bukkitService.getPlayerExact(playerName);
        if (player == null || !player.isOnline()) {
            // Player may be blocked in the pre-join dialog (Paper/Folia configuration phase).
            // Approving the force-login completes the blocking future so the player proceeds to
            // PLAY state, where AsynchronousJoin will call forceLogin() on their behalf.
            if (preJoinDialogService.approvePreJoinForceLogin(playerName.toLowerCase(Locale.ROOT))) {
                messages.send(sender, MessageKey.FORCE_LOGIN_SUCCESS, playerName);
            } else {
                messages.send(sender, MessageKey.FORCE_LOGIN_PLAYER_OFFLINE);
            }
        } else if (!permissionsManager.hasPermission(player, CAN_LOGIN_BE_FORCED)) {
            messages.send(sender, MessageKey.FORCE_LOGIN_FORBIDDEN, playerName);
        } else {
            management.forceLogin(player);
            messages.send(sender, MessageKey.FORCE_LOGIN_SUCCESS, playerName);
        }
    }
}
