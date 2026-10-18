package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.plugin.PluginManager;

import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Detects Bedrock players connected through Floodgate (Geyser). Floodgate has already verified
 * their Xbox account, so they can optionally be logged in (and registered) without a password.
 * <p>
 * Floodgate is accessed by reflection so that it stays an optional, runtime-only dependency.
 */
public class FloodgateService {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(FloodgateService.class);

    @Inject
    private PluginManager pluginManager;

    @Inject
    private Settings settings;

    FloodgateService() {
    }

    /**
     * Returns whether the player can be logged in automatically: the auto-login option is enabled
     * and Floodgate reports the UUID as a Bedrock player.
     *
     * @param playerId the UUID of the connecting player
     * @return true if the player should be logged in without a password
     */
    public boolean canAutoLogin(UUID playerId) {
        return settings.getProperty(HooksSettings.FLOODGATE_AUTO_LOGIN) && isFloodgatePlayer(playerId);
    }

    /**
     * Returns whether an unregistered player can be registered automatically (with a random password).
     *
     * @param playerId the UUID of the connecting player
     * @return true if the player should be registered and logged in without a password
     */
    public boolean canAutoRegister(UUID playerId) {
        return settings.getProperty(HooksSettings.FLOODGATE_AUTO_REGISTER) && canAutoLogin(playerId);
    }

    /**
     * Returns whether Floodgate reports the given UUID as a Bedrock player.
     *
     * @param playerId the UUID to check
     * @return true if Floodgate is installed and knows the player as a Bedrock player
     */
    public boolean isFloodgatePlayer(UUID playerId) {
        if (playerId == null || pluginManager.getPlugin("floodgate") == null) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Method isFloodgatePlayer = apiClass.getMethod("isFloodgatePlayer", UUID.class);
            return (boolean) isFloodgatePlayer.invoke(api, playerId);
        } catch (ReflectiveOperationException | RuntimeException e) {
            logger.logException("Could not check Floodgate for player " + playerId + ":", e);
            return false;
        }
    }
}
