package fr.xephi.authme.cache;

import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for handling tempbans
 */
public class TempbanManager implements SettingsDependent {

    private final ConcurrentHashMap<String, Integer> playerCounts;

    private final long MINUTE_IN_MILLISECONDS = 60000;

    private BukkitService bukkitService;

    private Messages messages;

    private boolean isEnabled;
    private int threshold;
    private int length;

    @Inject
    TempbanManager(NewSetting settings) {
        playerCounts = new ConcurrentHashMap<>();
        loadSettings(settings);
    }

    /**
     * Increases the failure count for the given player.
     *
     * @param name the player's name
     */
    public void increaseCount(String name) {
        if (isEnabled) {
            String nameLower = name.toLowerCase();
            Integer count = playerCounts.get(nameLower);

            if (count == null) {
                playerCounts.put(nameLower, 1);
            } else {
                playerCounts.put(nameLower, count + 1);
            }
        }
    }

    public void resetCount(String name) {
        if (isEnabled) {
            playerCounts.remove(name.toLowerCase());
        }
    }

    /**
     * Return whether the player should be tempbanned.
     *
     * @param name The player's name
     * @return True if the player should be tempbanned
     */
    public boolean shouldTempban(String name) {
        if (isEnabled) {
            Integer count = playerCounts.get(name.toLowerCase());
            return count != null && count >= threshold;
        }

        return false;
    }

    /**
     * Tempban a player for failing to log in too many times.
     * This bans the player's IP address, and calculates the expire
     * time based on the time the method was called.
     *
     * @param player The player to tempban
     */
    public void tempbanPlayer(Player player) {
        if (isEnabled) {
            resetCount(player.getName());

            final String ip = Utils.getPlayerIp(player);
            final String reason = messages.retrieveSingle(MessageKey.TEMPBAN_MAX_LOGINS);

            final Date expires = new Date();
            long newTime = expires.getTime() + (length * MINUTE_IN_MILLISECONDS);
            expires.setTime(newTime);

            bukkitService.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    Bukkit.getServer().getBanList(BanList.Type.IP).addBan(ip, reason, expires, "AuthMe");
                }
            });
        }
    }

    @Override
    public void loadSettings(NewSetting settings) {
        this.isEnabled = settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS);
        this.threshold = settings.getProperty(SecuritySettings.MAX_LOGIN_TEMPBAN);
        this.length = settings.getProperty(SecuritySettings.TEMPBAN_LENGTH);
    }
}
