package fr.xephi.authme.manager;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stateful manager for looking up IP address appropriately, including caching.
 */
public class IpAddressManager {

    /** Cache for IP lookups per player. */
    private final ConcurrentHashMap<String, String> ipCache;
    /** Whether or not to use the VeryGames API for IP lookups. */
    private final boolean useVeryGamesIpCheck;

    /**
     * Constructor.
     *
     * @param settings The settings instance
     */
    public IpAddressManager(NewSetting settings) {
        this.useVeryGamesIpCheck = settings.getProperty(HooksSettings.ENABLE_VERYGAMES_IP_CHECK);
        this.ipCache = new ConcurrentHashMap<>();
    }

    public String getPlayerIp(Player player) {
        final String playerName = player.getName().toLowerCase();
        final String cachedValue = ipCache.get(playerName);
        if (cachedValue != null) {
            return cachedValue;
        }

        final String plainIp = player.getAddress().getAddress().getHostAddress();
        if (useVeryGamesIpCheck) {
            String veryGamesResult = getVeryGamesIp(plainIp, player.getAddress().getPort());
            if (veryGamesResult != null) {
                ipCache.put(playerName, veryGamesResult);
                return veryGamesResult;
            }
        } else {
            ipCache.put(playerName, plainIp);
        }
        return plainIp;
    }

    public void addCache(String player, String ip) {
        ipCache.put(player.toLowerCase(), ip);
    }

    public void removeCache(String player) {
        ipCache.remove(player.toLowerCase());
    }

    // returns null if IP could not be looked up
    private String getVeryGamesIp(final String plainIp, final int port) {
        final String sUrl = String.format("http://monitor-1.verygames.net/api/?action=ipclean-real-ip"
            + "&out=raw&ip=%s&port=%d", plainIp, port);

        try {
            String result = Resources.toString(new URL(sUrl), Charsets.UTF_8);
            if (!StringUtils.isEmpty(result) && !result.contains("error")) {
                return result;
            }
        } catch (IOException e) {
            ConsoleLogger.logException("Could not fetch Very Games API with URL '" + sUrl + "':", e);
        }
        return null;
    }

}
