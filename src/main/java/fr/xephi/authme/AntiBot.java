package fr.xephi.authme;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;

/**
 * The AntiBot Service Management class.
 */
public class AntiBot {

    public enum AntiBotStatus {
        ARMED,
        DISARMED,
        DELAYED,
        ACTIVATED
    }

    private static AntiBotStatus antiBotStatus = AntiBotStatus.DISARMED;
    private static final AuthMe plugin = AuthMe.getInstance();
    private static final Messages messages = plugin.getMessages();
    private static final List<String> antibotPlayers = new ArrayList<>();

    public static void setupAntiBotService() {
        if (!Settings.enableAntiBot) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                antiBotStatus = AntiBotStatus.ARMED;
            }
        }, 2400);
    }

    public static void switchAntiBotStatus(boolean activated) {
        if(antiBotStatus == AntiBotStatus.DISARMED || antiBotStatus == AntiBotStatus.DELAYED) {
            return;
        }
        if(activated) {
            antiBotStatus = AntiBotStatus.ACTIVATED;
        } else {
            antiBotStatus = AntiBotStatus.ARMED;
        }
    }

    public static AntiBotStatus getAntiBotStatus() {
        return antiBotStatus;
    }

    public static void activateAntiBot() {
        antiBotStatus = AntiBotStatus.ACTIVATED;
        for (String s : messages.send("antibot_auto_enabled")) {
            Bukkit.broadcastMessage(s);
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (antiBotStatus == AntiBotStatus.ACTIVATED) {
                    antiBotStatus = AntiBotStatus.ARMED;
                    antibotPlayers.clear();
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
        if (antiBotStatus == AntiBotStatus.ACTIVATED || antiBotStatus == AntiBotStatus.DISARMED) {
            return;
        }
        if (plugin.getPermissionsManager().hasPermission(player, "authme.bypassantibot")) {
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

}
