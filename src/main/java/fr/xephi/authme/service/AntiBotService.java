package fr.xephi.authme.service;

import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
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

    // Settings
    private int duration;
    private int sensibility;
    private int delay;

    // Service status
    private AntiBotStatus antiBotStatus;
    private boolean startup;
    private BukkitTask disableTask;
    private int antibotPlayers;
    private final CopyOnWriteArrayList<String> antibotKicked = new CopyOnWriteArrayList<>();

    @Inject
    AntiBotService(Settings settings, Messages messages, PermissionsManager permissionsManager,
                   BukkitService bukkitService) {
        // Instances
        this.messages = messages;
        this.permissionsManager = permissionsManager;
        this.bukkitService = bukkitService;
        // Initial status
        disableTask = null;
        antibotPlayers = 0;
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
        delay = settings.getProperty(ProtectionSettings.ANTIBOT_DELAY);

        // Stop existing protection
        stopProtection();
        antiBotStatus = AntiBotStatus.DISABLED;

        // If antibot is disabled, just stop
        if (!settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)) {
            return;
        }

        // Bot activation task
        Runnable enableTask = new Runnable() {
            @Override
            public void run() {
                antiBotStatus = AntiBotStatus.LISTENING;
            }
        };

        // Delay the schedule on first start
        if(startup) {
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
        for (Player player : bukkitService.getOnlinePlayers()) {
            if (permissionsManager.hasPermission(player, AdminPermission.ANTIBOT_MESSAGES)) {
                messages.send(player, MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE);
            }
        }

        // Schedule auto-disable
        disableTask = bukkitService.runTaskLater(new Runnable() {
            @Override
            public void run() {
                stopProtection();
            }
        }, duration * TICKS_PER_MINUTE);
    }

    private void stopProtection() {
        if (antiBotStatus != AntiBotStatus.ACTIVE) {
            return;
        }

        // Change status
        antiBotStatus = AntiBotStatus.LISTENING;
        antibotPlayers = 0;
        antibotKicked.clear();

        // Cancel auto-disable task
        disableTask.cancel();
        disableTask = null;

        // Inform admins
        for (Player player : bukkitService.getOnlinePlayers()) {
            if (permissionsManager.hasPermission(player, AdminPermission.ANTIBOT_MESSAGES)) {
                messages.send(player, MessageKey.ANTIBOT_AUTO_DISABLED_MESSAGE, Integer.toString(duration));
            }
        }
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
     * Handles a player joining the server and checks if AntiBot needs to be activated.
     */
    public void handlePlayerJoin() {
        if (antiBotStatus != AntiBotStatus.LISTENING) {
            return;
        }

        antibotPlayers++;
        if (antibotPlayers > sensibility) {
            startProtection();
            return;
        }

        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                antibotPlayers--;
            }
        }, 5 * TICKS_PER_SECOND);
    }

    /**
     * Returns if a player should be kicked due to antibot service.
     *
     * @param isAuthAvailable if the player is registered
     * @return if the player should be kicked
     */
    public boolean shouldKick(boolean isAuthAvailable) {
        return !isAuthAvailable && (antiBotStatus == AntiBotStatus.ACTIVE);
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
