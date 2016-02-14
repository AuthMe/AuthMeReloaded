package fr.xephi.authme;

import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Wrapper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The AntiBot Service Management class.
 */
public class AntiBot {

    private static final Wrapper wrapper = Wrapper.getInstance();
    private static final AuthMe plugin = wrapper.getAuthMe();
    private static final Messages messages = wrapper.getMessages();
    private static final List<String> antibotPlayers = new ArrayList<>();
    private static AntiBotStatus antiBotStatus = AntiBotStatus.DISABLED;

    public static void setupAntiBotService() {
        if (!Settings.enableAntiBot) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                antiBotStatus = AntiBotStatus.LISTENING;
            }
        }, 2400);
    }

    public static void overrideAntiBotStatus(boolean activated) {
        if (antiBotStatus == AntiBotStatus.DISABLED) {
            return;
        }
        if (activated) {
            antiBotStatus = AntiBotStatus.ACTIVE;
        } else {
            antiBotStatus = AntiBotStatus.LISTENING;
        }
    }

    public static AntiBotStatus getAntiBotStatus() {
        return antiBotStatus;
    }

    public static void activateAntiBot() {
        antiBotStatus = AntiBotStatus.ACTIVE;
        for (String s : messages.retrieve(MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE)) {
            Bukkit.broadcastMessage(s);
        }

        wrapper.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (antiBotStatus == AntiBotStatus.ACTIVE) {
                    antiBotStatus = AntiBotStatus.LISTENING;
                    antibotPlayers.clear();
                    for (String s : messages.retrieve(MessageKey.ANTIBOT_AUTO_DISABLED_MESSAGE)) {
                        Bukkit.broadcastMessage(s.replace("%m", "" + Settings.antiBotDuration));
                    }
                }
            }
        }, Settings.antiBotDuration * 1200);
    }

    public static void checkAntiBot(final Player player) {
        if (antiBotStatus == AntiBotStatus.ACTIVE || antiBotStatus == AntiBotStatus.DISABLED) {
            return;
        }
        if (plugin.getPermissionsManager().hasPermission(player, PlayerStatePermission.BYPASS_ANTIBOT)) {
            return;
        }

        antibotPlayers.add(player.getName().toLowerCase());
        if (antibotPlayers.size() > Settings.antiBotSensibility) {
            activateAntiBot();
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
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
