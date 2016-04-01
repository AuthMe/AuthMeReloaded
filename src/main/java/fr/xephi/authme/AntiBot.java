package fr.xephi.authme;

import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_MINUTE;
import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;

/**
 * The AntiBot Service Management class.
 */
public class AntiBot {

    private final NewSetting settings;
    private final Messages messages;
    private final PermissionsManager permissionsManager;
    private final BukkitService bukkitService;
    private final List<String> antibotPlayers = new ArrayList<>();
    private AntiBotStatus antiBotStatus = AntiBotStatus.DISABLED;

    public AntiBot(NewSetting settings, Messages messages, PermissionsManager permissionsManager,
                   BukkitService bukkitService) {
        this.settings = settings;
        this.messages = messages;
        this.permissionsManager = permissionsManager;
        this.bukkitService = bukkitService;

        setupAntiBotService();
    }

    private void setupAntiBotService() {
        if (settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)) {
            bukkitService.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    antiBotStatus = AntiBotStatus.LISTENING;
                }
            }, 2 * TICKS_PER_MINUTE);
        }
    }

    public void overrideAntiBotStatus(boolean activated) {
        if (antiBotStatus != AntiBotStatus.DISABLED) {
            if (activated) {
                antiBotStatus = AntiBotStatus.ACTIVE;
            } else {
                antiBotStatus = AntiBotStatus.LISTENING;
            }
        }
    }

    public AntiBotStatus getAntiBotStatus() {
        return antiBotStatus;
    }

    public void activateAntiBot() {
        antiBotStatus = AntiBotStatus.ACTIVE;
        for (String s : messages.retrieve(MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE)) {
            bukkitService.broadcastMessage(s);
        }

        final int duration = settings.getProperty(ProtectionSettings.ANTIBOT_DURATION);
        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                if (antiBotStatus == AntiBotStatus.ACTIVE) {
                    antiBotStatus = AntiBotStatus.LISTENING;
                    antibotPlayers.clear();
                    for (String s : messages.retrieve(MessageKey.ANTIBOT_AUTO_DISABLED_MESSAGE)) {
                        bukkitService.broadcastMessage(s.replace("%m", Integer.toString(duration)));
                    }
                }
            }
        }, duration * TICKS_PER_MINUTE);
    }

    public void checkAntiBot(final Player player) {
        if (antiBotStatus == AntiBotStatus.ACTIVE || antiBotStatus == AntiBotStatus.DISABLED) {
            return;
        }
        if (permissionsManager.hasPermission(player, PlayerStatePermission.BYPASS_ANTIBOT)) {
            return;
        }

        antibotPlayers.add(player.getName().toLowerCase());
        if (antibotPlayers.size() > settings.getProperty(ProtectionSettings.ANTIBOT_SENSIBILITY)) {
            activateAntiBot();
            return;
        }
        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                antibotPlayers.remove(player.getName().toLowerCase());
            }
        }, 15 * TICKS_PER_SECOND);
    }

    public enum AntiBotStatus {
        LISTENING,
        DISABLED,
        ACTIVE
    }

}
