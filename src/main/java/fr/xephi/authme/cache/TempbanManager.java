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

import javax.inject.Inject;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manager for handling tempbans
 */
// TODO Gnat008 20160613: Figure out the best way to remove entries based on time
public class TempbanManager implements SettingsDependent {

    private final ConcurrentHashMap<String, Integer> ipLoginFailureCounts;

    private final long MINUTE_IN_MILLISECONDS = 60000;

    private BukkitService bukkitService;

    private Messages messages;

    private boolean isEnabled;
    private int threshold;
    private int length;

    @Inject
    TempbanManager(BukkitService bukkitService, Messages messages, NewSetting settings) {
        this.ipLoginFailureCounts = new ConcurrentHashMap<>();
        this.bukkitService = bukkitService;
        this.messages = messages;
        loadSettings(settings);
    }

    /**
     * Increases the failure count for the given IP address.
     *
     * @param address The player's IP address
     */
    public void increaseCount(String address) {
        if (isEnabled) {
            Integer count = ipLoginFailureCounts.get(address);

            if (count == null) {
                ipLoginFailureCounts.put(address, 1);
            } else {
                ipLoginFailureCounts.put(address, count + 1);
            }
        }
    }

    /**
     * Set the failure count for a given IP address to 0.
     *
     * @param address The IP address
     */
    public void resetCount(String address) {
        if (isEnabled) {
            ipLoginFailureCounts.remove(address);
        }
    }

    /**
     * Return whether the IP address should be tempbanned.
     *
     * @param address The player's IP address
     * @return True if the IP should be tempbanned
     */
    public boolean shouldTempban(String address) {
        if (isEnabled) {
            Integer count = ipLoginFailureCounts.get(address);
            return count != null && count >= threshold;
        }

        return false;
    }

    /**
     * Tempban a player's IP address for failing to log in too many times.
     * This calculates the expire time based on the time the method was called.
     *
     * @param player The player to tempban
     */
    public void tempbanPlayer(final Player player) {
        if (isEnabled) {
            final String ip = Utils.getPlayerIp(player);
            final String reason = messages.retrieveSingle(MessageKey.TEMPBAN_MAX_LOGINS);

            final Date expires = new Date();
            long newTime = expires.getTime() + (length * MINUTE_IN_MILLISECONDS);
            expires.setTime(newTime);

            bukkitService.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    Bukkit.getServer().getBanList(BanList.Type.IP).addBan(ip, reason, expires, "AuthMe");
                    player.kickPlayer(reason);
                }
            });

            resetCount(ip);
        }
    }

    @Override
    public void loadSettings(NewSetting settings) {
        this.isEnabled = settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS);
        this.threshold = settings.getProperty(SecuritySettings.MAX_LOGIN_TEMPBAN);
        this.length = settings.getProperty(SecuritySettings.TEMPBAN_LENGTH);
    }
}
