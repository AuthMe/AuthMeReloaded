package fr.xephi.authme.service;

import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CopyOnWriteArrayList;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_MINUTE;
import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;

/**
 * The AntiBot Service Management class.
 */
public class AntiBotService implements SettingsDependent {

    // Instances
    private final Messages messages;
    private final PermissionsManager permissionsManager;
    private final BukkitService bukkitService;
    private final CopyOnWriteArrayList<String> antibotKicked = new CopyOnWriteArrayList<>();
    // Settings
    private int duration;
    private int sensibility;
    private int interval;
    // Service status
    private AntiBotStatus antiBotStatus;
    private boolean startup;
    private BukkitTask disableTask;
    private Instant lastFlaggedJoin;
    private int flagged = 0;

    @Inject
    AntiBotService(Settings settings, Messages messages, PermissionsManager permissionsManager,
                   BukkitService bukkitService) {
        // Instances
        this.messages = messages;
        this.permissionsManager = permissionsManager;
        this.bukkitService = bukkitService;
        // Initial status
        disableTask = null;
        flagged = 0;
        antiBotStatus = AntiBotStatus.DISABLED;
        startup = true;
        // Load settings and start if required
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
        // Load settings
        duration = settings.getProperty(ProtectionSettings.ANTIBOT_DURATION);
        sensibility = settings.getProperty(ProtectionSettings.ANTIBOT_SENSIBILITY);
        interval = settings.getProperty(ProtectionSettings.ANTIBOT_INTERVAL);

        // Stop existing protection
        stopProtection();
        antiBotStatus = AntiBotStatus.DISABLED;

        // If antibot is disabled, just stop
        if (!settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)) {
            return;
        }

        // Bot activation task
        Runnable enableTask = () -> antiBotStatus = AntiBotStatus.LISTENING;

        // Delay the schedule on first start
        if (startup) {
            int delay = settings.getProperty(ProtectionSettings.ANTIBOT_DELAY);
            bukkitService.scheduleSyncDelayedTask(enableTask, delay * TICKS_PER_SECOND);
            startup = false;
        } else {
            enableTask.run();
        }
    }

    private void startProtection() {
        // Disable existing antibot session
        stopProtection();
        // Enable the new session
        antiBotStatus = AntiBotStatus.ACTIVE;

        // Inform admins
        bukkitService.getOnlinePlayers().stream()
            .filter(player -> permissionsManager.hasPermission(player, AdminPermission.ANTIBOT_MESSAGES))
            .forEach(player -> messages.send(player, MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE));

        // Schedule auto-disable
        disableTask = bukkitService.runTaskLater(this::stopProtection, duration * TICKS_PER_MINUTE);
    }

    /**
     * Transitions the anti bot service from active status back to listening.
     */
    private void stopProtection() {
        if (antiBotStatus != AntiBotStatus.ACTIVE) {
            return;
        }

        // Change status
        antiBotStatus = AntiBotStatus.LISTENING;
        flagged = 0;
        antibotKicked.clear();

        // Cancel auto-disable task
        disableTask.cancel();
        disableTask = null;

        // Inform admins
        String durationString = Integer.toString(duration);
        bukkitService.getOnlinePlayers().stream()
            .filter(player -> permissionsManager.hasPermission(player, AdminPermission.ANTIBOT_MESSAGES))
            .forEach(player -> messages.send(player, MessageKey.ANTIBOT_AUTO_DISABLED_MESSAGE, durationString));
    }

    /**
     * Returns the status of the AntiBot service.
     *
     * @return status of the antibot service
     */
    public AntiBotStatus getAntiBotStatus() {
        return antiBotStatus;
    }

    /**
     * Allows to override the status of the protection.
     *
     * @param started the new protection status
     */
    public void overrideAntiBotStatus(boolean started) {
        if (antiBotStatus != AntiBotStatus.DISABLED) {
            if (started) {
                startProtection();
            } else {
                stopProtection();
            }
        }
    }

    /**
     * Returns if a player should be kicked due to antibot service.
     *
     * @return if the player should be kicked
     */
    public boolean shouldKick() {
        if (antiBotStatus == AntiBotStatus.DISABLED) {
            return false;
        } else if (antiBotStatus == AntiBotStatus.ACTIVE) {
            return true;
        }

        if (lastFlaggedJoin == null) {
            lastFlaggedJoin = Instant.now();
        }
        if (ChronoUnit.SECONDS.between(lastFlaggedJoin, Instant.now()) <= interval) {
            flagged++;
        } else {
            // reset to 1 because this player is also count as not registered
            flagged = 1;
            lastFlaggedJoin = null;
        }
        if (flagged > sensibility) {
            startProtection();
            return true;
        }
        return false;
    }

    /**
     * Returns whether the player was kicked because of activated antibot. The list is reset
     * when antibot is deactivated.
     *
     * @param name the name to check
     *
     * @return true if the given name has been kicked because of Antibot
     */
    public boolean wasPlayerKicked(String name) {
        return antibotKicked.contains(name.toLowerCase());
    }

    /**
     * Adds a name to the list of players kicked by antibot. Should only be used when a player
     * is determined to be kicked because of failed antibot verification.
     *
     * @param name the name to add
     */
    public void addPlayerKick(String name) {
        antibotKicked.addIfAbsent(name.toLowerCase());
    }

    public enum AntiBotStatus {
        LISTENING,
        DISABLED,
        ACTIVE
    }

}
