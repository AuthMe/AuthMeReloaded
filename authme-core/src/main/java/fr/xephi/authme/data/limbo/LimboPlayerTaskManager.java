package fr.xephi.authme.data.limbo;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.captcha.RegistrationCaptchaManager;
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

    @Inject
    private RegistrationCaptchaManager registrationCaptchaManager;

    LimboPlayerTaskManager() {
    }

    /**
     * Registers a {@link MessageTask} for the given player name.
     *
     * @param player the player
     * @param limbo the associated limbo player of the player
     * @param messageType message type
     */
    void registerMessageTask(Player player, LimboPlayer limbo, LimboMessageType messageType) {
        int interval = settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL);
        MessageResult result = getMessageKey(player.getName(), messageType);
        if (interval > 0) {
            String[] joinMessage = messages.retrieveSingle(player, result.messageKey, result.args).split("\n");
            MessageTask messageTask = new MessageTask(player, joinMessage);
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
            String message = messages.retrieveSingle(player, MessageKey.LOGIN_TIMEOUT_ERROR);
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
     * @param name the player's name
     * @param messageType the message to show
     * @return the message key to display to the user
     */
    private MessageResult getMessageKey(String name, LimboMessageType messageType) {
        if (messageType == LimboMessageType.LOG_IN) {
            return new MessageResult(MessageKey.LOGIN_MESSAGE);
        } else if (messageType == LimboMessageType.TOTP_CODE) {
            return new MessageResult(MessageKey.TWO_FACTOR_CODE_REQUIRED);
        } else if (registrationCaptchaManager.isCaptchaRequired(name)) {
            final String captchaCode = registrationCaptchaManager.getCaptchaCodeOrGenerateNew(name);
            return new MessageResult(MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED, captchaCode);
        } else {
            return new MessageResult(MessageKey.REGISTER_MESSAGE);
        }
    }

    private static final class MessageResult {
        private final MessageKey messageKey;
        private final String[] args;

        MessageResult(MessageKey messageKey, String... args) {
            this.messageKey = messageKey;
            this.args = args;
        }
    }
}
