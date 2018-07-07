package fr.xephi.authme.data;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.expiring.TimedCounter;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static fr.xephi.authme.util.Utils.MILLIS_PER_MINUTE;

/**
 * Manager for handling temporary bans.
 */
public class TempbanManager implements SettingsDependent, HasCleanup {

    private final Map<String, TimedCounter<String>> ipLoginFailureCounts;
    private final BukkitService bukkitService;
    private final Messages messages;

    private boolean isEnabled;
    private int threshold;
    private int length;
    private long resetThreshold;
    private String customCommand;

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
            TimedCounter<String> countsByName = ipLoginFailureCounts.computeIfAbsent(
                address, k -> new TimedCounter<>(resetThreshold, TimeUnit.MINUTES));
            countsByName.increment(name);
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
            TimedCounter<String> counter = ipLoginFailureCounts.get(address);
            if (counter != null) {
                counter.remove(name);
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
            TimedCounter<String> countsByName = ipLoginFailureCounts.get(address);
            if (countsByName != null) {
                return countsByName.total() >= threshold;
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
            final String name = player.getName();
            final String ip = PlayerUtils.getPlayerIp(player);
            final String reason = messages.retrieveSingle(player, MessageKey.TEMPBAN_MAX_LOGINS);

            final Date expires = new Date();
            long newTime = expires.getTime() + (length * MILLIS_PER_MINUTE);
            expires.setTime(newTime);

            bukkitService.scheduleSyncDelayedTask(() -> {
                if(customCommand.isEmpty()) {
                    bukkitService.banIp(ip, reason, expires, "AuthMe");
                    player.kickPlayer(reason);
                } else {
                    String command = customCommand
                        .replace("%player%", name)
                        .replace("%ip%", ip);
                    bukkitService.dispatchConsoleCommand(command);
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
        this.resetThreshold = settings.getProperty(SecuritySettings.TEMPBAN_MINUTES_BEFORE_RESET);
        this.customCommand = settings.getProperty(SecuritySettings.TEMPBAN_CUSTOM_COMMAND);
    }

    @Override
    public void performCleanup() {
        for (TimedCounter<String> countsByIp : ipLoginFailureCounts.values()) {
            countsByIp.removeExpiredEntries();
        }
        ipLoginFailureCounts.entrySet().removeIf(e -> e.getValue().isEmpty());
    }
}
