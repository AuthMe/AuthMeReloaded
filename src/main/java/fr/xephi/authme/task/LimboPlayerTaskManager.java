package fr.xephi.authme.task;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;

/**
 * Registers tasks associated with a PlayerData.
 */
public class LimboPlayerTaskManager {

    @Inject
    private Messages messages;

    @Inject
    private Settings settings;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private LimboCache limboCache;

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
            final LimboPlayer limboPlayer = limboCache.getPlayerData(name);
            if (limboPlayer == null) {
                ConsoleLogger.info("PlayerData for '" + name + "' is not available");
            } else {
                cancelTask(limboPlayer.getMessageTask());
                BukkitTask messageTask = bukkitService.runTask(new MessageTask(name, messages.retrieve(key),
                    interval, bukkitService, limboCache, playerCache));
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
            final LimboPlayer limboPlayer = limboCache.getPlayerData(player.getName());
            if (limboPlayer == null) {
                ConsoleLogger.info("PlayerData for '" + player.getName() + "' is not available");
            } else {
                cancelTask(limboPlayer.getTimeoutTask());
                String message = messages.retrieveSingle(MessageKey.LOGIN_TIMEOUT_ERROR);
                BukkitTask task = bukkitService.runTaskLater(new TimeoutTask(player, message, playerCache), timeout);
                limboPlayer.setTimeoutTask(task);
            }
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
            return settings.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)
                ? MessageKey.REGISTER_EMAIL_MESSAGE
                : MessageKey.REGISTER_MESSAGE;
        }
    }

    /**
     * Null-safe method to cancel a potentially existing task.
     *
     * @param task the task to cancel (or null)
     */
    private static void cancelTask(BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
    }
}
