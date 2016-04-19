package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Admin command to unregister a player.
 */
public class UnregisterAdminCommand implements ExecutableCommand {


    @Override
    public void executeCommand(final CommandSender sender, List<String> arguments, CommandService commandService) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Get the player name
        String playerName = arguments.get(0);
        String playerNameLowerCase = playerName.toLowerCase();

        // Make sure the user is valid
        if (!commandService.getDataSource().isAuthAvailable(playerNameLowerCase)) {
            commandService.send(sender, MessageKey.UNKNOWN_USER);
            return;
        }

        // Remove the player
        if (!commandService.getDataSource().removeAuth(playerNameLowerCase)) {
            commandService.send(sender, MessageKey.ERROR);
            return;
        }

        // Unregister the player
        Player target = Utils.getPlayer(playerNameLowerCase);
        PlayerCache.getInstance().removePlayer(playerNameLowerCase);
        Utils.setGroup(target, Utils.GroupType.UNREGISTERED);
        if (target != null && target.isOnline()) {
            Utils.teleportToSpawn(target);
            LimboCache.getInstance().addLimboPlayer(target);
            int timeOut = Settings.getRegistrationTimeout * 20;
            int interval = Settings.getWarnMessageInterval;
            BukkitScheduler scheduler = sender.getServer().getScheduler();
            if (timeOut != 0) {
                BukkitTask id = scheduler.runTaskLater(plugin, new TimeoutTask(plugin, playerNameLowerCase, target), timeOut);
                LimboCache.getInstance().getLimboPlayer(playerNameLowerCase).setTimeoutTask(id);
            }
            LimboCache.getInstance().getLimboPlayer(playerNameLowerCase).setMessageTask(
                scheduler.runTask(
                    plugin, new MessageTask(plugin, playerNameLowerCase, MessageKey.REGISTER_MESSAGE, interval)
                )
            );
            if (commandService.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeOut, 2));
            }
            commandService.send(target, MessageKey.UNREGISTERED_SUCCESS);
        }

        // Show a status message
        commandService.send(sender, MessageKey.UNREGISTERED_SUCCESS);
        ConsoleLogger.info(playerName + " unregistered");
    }
}
