package fr.xephi.authme.data;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.settings.properties.SecuritySettings.TEMPBAN_MINUTES_BEFORE_RESET;
import static fr.xephi.authme.util.Utils.MILLIS_PER_MINUTE;

/**
 * Manager for handling temporary bans.
 */
public class TempbanManager implements SettingsDependent, HasCleanup {

    private final Map<String, Map<String, TimedCounter>> ipLoginFailureCounts;
    private final BukkitService bukkitService;
    private final Messages messages;

    private boolean isEnabled;
    private int threshold;
    private int length;
    private long resetThreshold;

    @Inject
    TempbanManager(BukkitService bukkitService, Messages messages, Settings settings) {
        this.ipLoginFailureCounts = new ConcurrentHashMap<>();
        this.bukkitService = bukkitService;
        this.messages = messages;
        reload(settings);
    }

    /**
     * Increases the failure count for the given IP address/username combination.
     *
     * @param address The player's IP address
     * @param name The username
     */
    public void increaseCount(String address, String name) {
        if (isEnabled) {
            Map<String, TimedCounter> countsByName = ipLoginFailureCounts.get(address);
            if (countsByName == null) {
                countsByName = new ConcurrentHashMap<>();
                ipLoginFailureCounts.put(address, countsByName);
            }

            TimedCounter counter = countsByName.get(name);
            if (counter == null) {
                countsByName.put(name, new TimedCounter(1));
            } else {
                counter.increment(resetThreshold);
            }
        }
    }

    /**
     * Set the failure count for a given IP address / username combination to 0.
     *
     * @param address The IP address
     * @param name The username
     */
    public void resetCount(String address, String name) {
        if (isEnabled) {
            Map<String, TimedCounter> map = ipLoginFailureCounts.get(address);
            if (map != null) {
                map.remove(name);
            }
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
            Map<String, TimedCounter> countsByName = ipLoginFailureCounts.get(address);
            if (countsByName != null) {
                int total = 0;
                for (TimedCounter counter : countsByName.values()) {
                    total += counter.getCount(resetThreshold);
                }
                return total >= threshold;
            }
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
            final String ip = PlayerUtils.getPlayerIp(player);
            final String reason = messages.retrieveSingle(MessageKey.TEMPBAN_MAX_LOGINS);

            final Date expires = new Date();
            long newTime = expires.getTime() + (length * MILLIS_PER_MINUTE);
            expires.setTime(newTime);

            bukkitService.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    bukkitService.banIp(ip, reason, expires, "AuthMe");
                    player.kickPlayer(reason);
                }
            });

            ipLoginFailureCounts.remove(ip);
        }
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabled = settings.getProperty(SecuritySettings.TEMPBAN_ON_MAX_LOGINS);
        this.threshold = settings.getProperty(SecuritySettings.MAX_LOGIN_TEMPBAN);
        this.length = settings.getProperty(SecuritySettings.TEMPBAN_LENGTH);
        this.resetThreshold = settings.getProperty(TEMPBAN_MINUTES_BEFORE_RESET) * MILLIS_PER_MINUTE;
    }

    @Override
    public void performCleanup() {
        for (Map<String, TimedCounter> countsByIp : ipLoginFailureCounts.values()) {
            Iterator<TimedCounter> it = countsByIp.values().iterator();
            while (it.hasNext()) {
                TimedCounter counter = it.next();
                if (counter.getCount(resetThreshold) == 0) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Counter with an associated timestamp, keeping track of when the last entry has been added.
     */
    @VisibleForTesting
    static final class TimedCounter {

        private int counter;
        private long lastIncrementTimestamp = System.currentTimeMillis();

        /**
         * Constructor.
         *
         * @param start the initial value to set the counter to
         */
        TimedCounter(int start) {
            this.counter = start;
        }

        /**
         * Returns the count, taking into account the last entry timestamp.
         *
         * @param threshold the threshold in milliseconds until when to consider a counter
         * @return the counter's value, or {@code 0} if it was last incremented longer ago than the threshold
         */
        int getCount(long threshold) {
            if (System.currentTimeMillis() - lastIncrementTimestamp > threshold) {
                return 0;
            }
            return counter;
        }

        /**
         * Increments the counter, taking into account the last entry timestamp.
         *
         * @param threshold in milliseconds, the time span until which to consider the existing number
         */
        void increment(long threshold) {
            counter = getCount(threshold) + 1;
            lastIncrementTimestamp = System.currentTimeMillis();
        }
    }
}
