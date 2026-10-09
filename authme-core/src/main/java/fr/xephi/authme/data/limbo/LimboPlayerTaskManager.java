package fr.xephi.authme.data.limbo;

import ch.jalu.configme.properties.Property;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.captcha.RegistrationCaptchaManager;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CancellableTask;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    @Inject
    private DataSource dataSource;

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
            CancellableTask taskHandle = messageType == LimboMessageType.REGISTER
                ? scheduleRegistrationMessageTask(player, messageTask, interval)
                : bukkitService.runTaskTimer(player, messageTask, 2 * TICKS_PER_SECOND,
                    interval * TICKS_PER_SECOND);
            limbo.setMessageTask(messageTask, taskHandle);
        }
    }

    private CancellableTask scheduleRegistrationMessageTask(Player player, MessageTask messageTask, int interval) {
        int configuredDelay = settings.getProperty(RegistrationSettings.REGISTER_MESSAGE_DELAY);
        long delayTicks = Math.max(0, configuredDelay) * (long) TICKS_PER_SECOND;
        long intervalTicks = interval * (long) TICKS_PER_SECOND;
        RegistrationMessageTaskHandle taskHandle =
            new RegistrationMessageTaskHandle(player, messageTask, intervalTicks);
        taskHandle.scheduleInitialCheck(delayTicks);
        return taskHandle;
    }

    /**
     * Registers a {@link TimeoutTask} for the given player according to the configuration.
     *
     * @param player the player to register a timeout task for
     * @param limbo the associated limbo player
     * @param messageType the limbo message type, used to select login vs. register timeout
     */
    void registerTimeoutTask(Player player, LimboPlayer limbo, LimboMessageType messageType) {
        Property<Integer> timeoutProperty = messageType == LimboMessageType.REGISTER
            ? RestrictionSettings.REGISTER_TIMEOUT
            : RestrictionSettings.LOGIN_TIMEOUT;
        final int timeout = settings.getProperty(timeoutProperty) * TICKS_PER_SECOND;
        if (timeout > 0) {
            String message = messages.retrieveSingle(player, MessageKey.LOGIN_TIMEOUT_ERROR);
            CancellableTask task = bukkitService.runTaskLater(player, new TimeoutTask(player, message, playerCache), timeout);
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

    private final class RegistrationMessageTaskHandle implements CancellableTask {
        private final Player player;
        private final MessageTask messageTask;
        private final long intervalTicks;
        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicReference<CancellableTask> delayedTask = new AtomicReference<>();
        private final AtomicReference<CancellableTask> lookupTask = new AtomicReference<>();
        private final AtomicReference<CancellableTask> repeatingTask = new AtomicReference<>();

        private RegistrationMessageTaskHandle(Player player, MessageTask messageTask, long intervalTicks) {
            this.player = player;
            this.messageTask = messageTask;
            this.intervalTicks = intervalTicks;
        }

        private void scheduleInitialCheck(long delayTicks) {
            CancellableTask task = bukkitService.runTaskLater(player, this::checkRegistrationAsync, delayTicks);
            setTask(delayedTask, task);
        }

        private void checkRegistrationAsync() {
            if (cancelled.get() || !player.isOnline() || playerCache.isAuthenticated(player.getName())) {
                return;
            }

            CancellableTask task = bukkitService.runTaskAsynchronously(() -> {
                boolean isRegistered = dataSource.isAuthAvailable(player.getName());
                if (cancelled.get() || isRegistered || playerCache.isAuthenticated(player.getName())) {
                    return;
                }

                bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, () -> {
                    if (cancelled.get() || !player.isOnline() || playerCache.isAuthenticated(player.getName())) {
                        return;
                    }

                    messageTask.run();
                    CancellableTask repeating = bukkitService.runTaskTimer(
                        player, messageTask, intervalTicks, intervalTicks);
                    setTask(repeatingTask, repeating);
                });
            });
            setTask(lookupTask, task);
        }

        private void setTask(AtomicReference<CancellableTask> taskReference, CancellableTask task) {
            if (cancelled.get()) {
                task.cancel();
            } else {
                taskReference.set(task);
                if (cancelled.get() && taskReference.compareAndSet(task, null)) {
                    task.cancel();
                }
            }
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                cancelTask(delayedTask);
                cancelTask(lookupTask);
                cancelTask(repeatingTask);
            }
        }

        private void cancelTask(AtomicReference<CancellableTask> taskReference) {
            CancellableTask task = taskReference.getAndSet(null);
            if (task != null) {
                task.cancel();
            }
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
