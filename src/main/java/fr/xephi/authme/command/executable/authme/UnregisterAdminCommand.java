package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.command.CommandParts;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 * Admin command to unregister a player.
 */
public class UnregisterAdminCommand extends ExecutableCommand {

    /**
     * Execute the command.
     *
     * @param sender           The command sender.
     * @param commandReference The command reference.
     * @param commandArguments The command arguments.
     *
     * @return True if the command was executed successfully, false otherwise.
     */
    @Override
    public boolean executeCommand(final CommandSender sender, CommandParts commandReference, CommandParts commandArguments) {
        // AuthMe plugin instance
        final AuthMe plugin = AuthMe.getInstance();

        // Messages instance
        final Messages m = plugin.getMessages();

        // Get the player name
        String playerName = commandArguments.get(0);
        String playerNameLowerCase = playerName.toLowerCase();

        // Make sure the user is valid
        if (!plugin.database.isAuthAvailable(playerNameLowerCase)) {
            m.send(sender, MessageKey.UNKNOWN_USER);
            return true;
        }

        // Remove the player
        if (!plugin.database.removeAuth(playerNameLowerCase)) {
            m.send(sender, MessageKey.ERROR);
            return true;
        }

        // Unregister the player
        Player target = Utils.getPlayer(playerNameLowerCase);
        PlayerCache.getInstance().removePlayer(playerNameLowerCase);
        Utils.setGroup(target, Utils.GroupType.UNREGISTERED);
        if (target != null && target.isOnline()) {
            Utils.teleportToSpawn(target);
            LimboCache.getInstance().addLimboPlayer(target);
            int delay = Settings.getRegistrationTimeout * 20;
            int interval = Settings.getWarnMessageInterval;
            BukkitScheduler scheduler = sender.getServer().getScheduler();
            if (delay != 0) {
                BukkitTask id = scheduler.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, playerNameLowerCase, target), delay);
                LimboCache.getInstance().getLimboPlayer(playerNameLowerCase).setTimeoutTaskId(id);
            }
            LimboCache.getInstance().getLimboPlayer(playerNameLowerCase).setMessageTaskId(
                scheduler.runTaskAsynchronously(plugin,
                    new MessageTask(plugin, playerNameLowerCase, m.retrieve(MessageKey.REGISTER_MESSAGE), interval)));
            if (Settings.applyBlindEffect) {
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS,
                    Settings.getRegistrationTimeout * 20, 2));
            }
            m.send(target, MessageKey.UNREGISTERED_SUCCESS);

        }

        // Show a status message
        m.send(sender, MessageKey.UNREGISTERED_SUCCESS);
        ConsoleLogger.info(playerName + " unregistered");
        return true;
    }
}
