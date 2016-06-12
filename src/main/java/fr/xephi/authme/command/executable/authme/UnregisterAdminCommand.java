package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
import java.util.List;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;

/**
 * Admin command to unregister a player.
 */
public class UnregisterAdminCommand implements ExecutableCommand {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommandService commandService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private AuthMe authMe;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private LimboCache limboCache;

    @Inject
    private PermissionsManager permissionsManager;

    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments) {
        // Get the player name
        String playerName = arguments.get(0);
        String playerNameLowerCase = playerName.toLowerCase();

        // Make sure the user is valid
        if (!dataSource.isAuthAvailable(playerNameLowerCase)) {
            commandService.send(sender, MessageKey.UNKNOWN_USER);
            return;
        }

        // Remove the player
        if (!dataSource.removeAuth(playerNameLowerCase)) {
            commandService.send(sender, MessageKey.ERROR);
            return;
        }

        // Unregister the player
        Player target = bukkitService.getPlayerExact(playerNameLowerCase);
        playerCache.removePlayer(playerNameLowerCase);
        permissionsManager.setGroup(target, AuthGroupType.UNREGISTERED);
        if (target != null && target.isOnline()) {
            if (commandService.getProperty(RegistrationSettings.FORCE)) {
                applyUnregisteredEffectsAndTasks(target);
            }
            commandService.send(target, MessageKey.UNREGISTERED_SUCCESS);
        }

        // Show a status message
        commandService.send(sender, MessageKey.UNREGISTERED_SUCCESS);
        ConsoleLogger.info(sender.getName() + " unregistered " + playerName);
    }

    /**
     * When registration is forced, applies the configured "unregistered effects" to the player as he
     * would encounter when joining the server before logging on - reminder task to log in,
     * timeout kick, blindness.
     *
     * @param target the player that was unregistered
     */
    private void applyUnregisteredEffectsAndTasks(Player target) {
        final String playerNameLowerCase = target.getName().toLowerCase();

        Utils.teleportToSpawn(target);
        limboCache.addLimboPlayer(target);
        int timeOut = commandService.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
        int interval = commandService.getProperty(RegistrationSettings.MESSAGE_INTERVAL);
        if (timeOut != 0) {
            BukkitTask id = bukkitService.runTaskLater(new TimeoutTask(authMe, playerNameLowerCase, target), timeOut);
            limboCache.getLimboPlayer(playerNameLowerCase).setTimeoutTask(id);
        }
        limboCache.getLimboPlayer(playerNameLowerCase).setMessageTask(
            bukkitService.runTask(new MessageTask(bukkitService, authMe.getMessages(),
                playerNameLowerCase, MessageKey.REGISTER_MESSAGE, interval)));

        if (commandService.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeOut, 2));
        }
    }
}
