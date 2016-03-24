package fr.xephi.authme;

import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The AntiBot Service Management class.
 */
public class AntiBot {

    private final Messages messages;
    private final PermissionsManager permissionsManager;
    private final BukkitService bukkitService;
    private final List<String> antibotPlayers = new ArrayList<>();
    private AntiBotStatus antiBotStatus = AntiBotStatus.DISABLED;

    public AntiBot(Messages messages, PermissionsManager permissionsManager, BukkitService bukkitService) {
        this.messages = messages;
        this.permissionsManager = permissionsManager;
        this.bukkitService = bukkitService;

        setupAntiBotService();
    }

    private void setupAntiBotService() {
        if (!Settings.enableAntiBot) {
            return;
        }
        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                antiBotStatus = AntiBotStatus.LISTENING;
            }
        }, 2400);
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
            Bukkit.broadcastMessage(s);
        }

        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                if (antiBotStatus == AntiBotStatus.ACTIVE) {
                    antiBotStatus = AntiBotStatus.LISTENING;
                    antibotPlayers.clear();
                    for (String s : messages.retrieve(MessageKey.ANTIBOT_AUTO_DISABLED_MESSAGE)) {
                        bukkitService.broadcastMessage(s.replace("%m", Integer.toString(Settings.antiBotDuration)));
                    }
                }
            }
        }, Settings.antiBotDuration * 1200);
    }

    public void checkAntiBot(final Player player) {
        if (antiBotStatus == AntiBotStatus.ACTIVE || antiBotStatus == AntiBotStatus.DISABLED) {
            return;
        }
        if (permissionsManager.hasPermission(player, PlayerStatePermission.BYPASS_ANTIBOT)) {
            return;
        }

        antibotPlayers.add(player.getName().toLowerCase());
        if (antibotPlayers.size() > Settings.antiBotSensibility) {
            activateAntiBot();
            return;
        }
        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                antibotPlayers.remove(player.getName().toLowerCase());
            }
        }, 300);
    }

    public enum AntiBotStatus {
        LISTENING,
        DISABLED,
        ACTIVE
    }

}
