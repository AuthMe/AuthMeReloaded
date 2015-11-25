package fr.xephi.authme;

import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * The AntiBot Service Management class.
 */
public class AntiBot {

    private static final AuthMe plugin = AuthMe.getInstance();
    private static final Messages messages = plugin.getMessages();
    private static final List<String> antiBotPlayers = new ArrayList<>();
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
        for (String s : messages.send("antibot_auto_enabled")) {
            Bukkit.broadcastMessage(s);
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (antiBotStatus == AntiBotStatus.ACTIVE) {
                    antiBotStatus = AntiBotStatus.LISTENING;
                    antiBotPlayers.clear();
                    for (String s : messages.send("antibot_auto_disabled"))
                        Bukkit.broadcastMessage(s.replace("%m", "" + Settings.antiBotDuration));
                }
            }
        }, Settings.antiBotDuration * 1200);
    }

    /**
     * Method checkAntiBotMod.
     *
     * @param player Player
     */
    public static void checkAntiBot(final Player player) {
        if (antiBotStatus == AntiBotStatus.ACTIVE || antiBotStatus == AntiBotStatus.DISABLED) {
            return;
        }
        if (plugin.getPermissionsManager().hasPermission(player, "authme.bypassantibot")) {
            return;
        }

        antiBotPlayers.add(player.getName().toLowerCase());
        if (antiBotPlayers.size() > Settings.antiBotSensibility) {
            activateAntiBot();
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                antiBotPlayers.remove(player.getName().toLowerCase());
            }
        }, 300);
    }

    public enum AntiBotStatus {
        LISTENING,
        DISABLED,
        ACTIVE
    }

}
