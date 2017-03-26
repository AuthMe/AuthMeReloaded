package fr.xephi.authme.data.limbo;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;

/**
 * Registers tasks associated with a LimboPlayer.
 */
class LimboPlayerTaskManager {

    @Inject
    private Messages messages;

    @Inject
    private Settings settings;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PlayerCache playerCache;

    LimboPlayerTaskManager() {
    }

    /**
     * Registers a {@link MessageTask} for the given player name.
     *
     * @param player the player
     * @param limbo the associated limbo player of the player
     * @param isRegistered whether the player is registered or not
     *                     (false shows "please register", true shows "please log in")
     */
    void registerMessageTask(Player player, LimboPlayer limbo, boolean isRegistered) {
        int interval = settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL);
        MessageKey key = getMessageKey(isRegistered);
        if (interval > 0) {
            MessageTask messageTask = new MessageTask(player, messages.retrieve(key));
            bukkitService.runTaskTimer(messageTask, 2 * TICKS_PER_SECOND, interval * TICKS_PER_SECOND);
            limbo.setMessageTask(messageTask);
        }
    }

    /**
     * Registers a {@link TimeoutTask} for the given player according to the configuration.
     *
     * @param player the player to register a timeout task for
     * @param limbo the associated limbo player
     */
    void registerTimeoutTask(Player player, LimboPlayer limbo) {
        final int timeout = settings.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
        if (timeout > 0) {
            String message = messages.retrieveSingle(MessageKey.LOGIN_TIMEOUT_ERROR);
            BukkitTask task = bukkitService.runTaskLater(new TimeoutTask(player, message, playerCache), timeout);
            limbo.setTimeoutTask(task);
        }
    }

    /**
     * Null-safe method to set the muted flag on a message task.
     *
     * @param task the task to modify (or null)
     * @param isMuted the value to set if task is not null
     */
    static void setMuted(MessageTask task, boolean isMuted) {
        if (task != null) {
            task.setMuted(isMuted);
        }
    }

    /**
     * Returns the appropriate message key according to the registration status and settings.
     *
     * @param isRegistered whether or not the username is registered
     * @return the message key to display to the user
     */
    private static MessageKey getMessageKey(boolean isRegistered) {
        if (isRegistered) {
            return MessageKey.LOGIN_MESSAGE;
        } else {
            return MessageKey.REGISTER_MESSAGE;
        }
    }
}
