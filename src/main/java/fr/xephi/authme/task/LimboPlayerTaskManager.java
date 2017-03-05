package fr.xephi.authme.task;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;

/**
 * Registers tasks associated with a LimboPlayer.
 */
public class LimboPlayerTaskManager {

    @Inject
    private Messages messages;

    @Inject
    private Settings settings;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private LimboService limboService;

    @Inject
    private PlayerCache playerCache;

    LimboPlayerTaskManager() {
    }


    /**
     * Registers a {@link MessageTask} for the given player name.
     *
     * @param name the name of the player to schedule a repeating message task for
     * @param isRegistered whether the name is registered or not
     *                     (false shows "please register", true shows "please log in")
     */
    public void registerMessageTask(String name, boolean isRegistered) {
        final int interval = settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL);
        final MessageKey key = getMessageKey(isRegistered);
        if (interval > 0) {
            final LimboPlayer limboPlayer = limboService.getLimboPlayer(name);
            if (limboPlayer == null) {
                ConsoleLogger.info("LimboPlayer for '" + name + "' is not available (MessageTask)");
            } else {
                MessageTask messageTask = new MessageTask(name, messages.retrieve(key), bukkitService, playerCache);
                bukkitService.runTaskTimer(messageTask, 2 * TICKS_PER_SECOND, interval * TICKS_PER_SECOND);
                limboPlayer.setMessageTask(messageTask);
            }
        }
    }

    /**
     * Registers a {@link TimeoutTask} for the given player according to the configuration.
     *
     * @param player the player to register a timeout task for
     */
    public void registerTimeoutTask(Player player) {
        final int timeout = settings.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
        if (timeout > 0) {
            final LimboPlayer limboPlayer = limboService.getLimboPlayer(player.getName());
            if (limboPlayer == null) {
                ConsoleLogger.info("LimboPlayer for '" + player.getName() + "' is not available (TimeoutTask)");
            } else {
                String message = messages.retrieveSingle(MessageKey.LOGIN_TIMEOUT_ERROR);
                BukkitTask task = bukkitService.runTaskLater(new TimeoutTask(player, message, playerCache), timeout);
                limboPlayer.setTimeoutTask(task);
            }
        }
    }

    public void muteMessageTask(Player player) {
        LimboPlayer limbo = limboService.getLimboPlayer(player.getName());
        if (limbo != null) {
            setMuted(limbo.getMessageTask(), true);
        }
    }

    public void unmuteMessageTask(Player player) {
        LimboPlayer limbo = limboService.getLimboPlayer(player.getName());
        if (limbo != null) {
            setMuted(limbo.getMessageTask(), false);
        }
    }

    public void clearTasks(Player player) {
        LimboPlayer limbo = limboService.getLimboPlayer(player.getName());
        if (limbo != null) {
            limbo.clearTasks();
        }
    }

    /**
     * Returns the appropriate message key according to the registration status and settings.
     *
     * @param isRegistered whether or not the username is registered
     * @return the message key to display to the user
     */
    private MessageKey getMessageKey(boolean isRegistered) {
        if (isRegistered) {
            return MessageKey.LOGIN_MESSAGE;
        } else {
            return MessageKey.REGISTER_MESSAGE;
        }
    }

    /**
     * Null-safe method to set the muted flag on a message task.
     *
     * @param task the task to modify (or null)
     * @param isMuted the value to set if task is not null
     */
    private static void setMuted(MessageTask task, boolean isMuted) {
        if (task != null) {
            task.setMuted(isMuted);
        }
    }
}
