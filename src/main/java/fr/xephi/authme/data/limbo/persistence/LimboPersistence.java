package fr.xephi.authme.data.limbo.persistence;

import ch.jalu.injector.factory.Factory;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.LimboSettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Handles the persistence of LimboPlayers.
 */
public class LimboPersistence implements SettingsDependent {

    private final Factory<LimboPersistenceHandler> handlerFactory;

    private LimboPersistenceHandler handler;

    @Inject
    LimboPersistence(Settings settings, Factory<LimboPersistenceHandler> handlerFactory) {
        this.handlerFactory = handlerFactory;
        reload(settings);
    }

    /**
     * Retrieves the LimboPlayer for the given player if available.
     *
     * @param player the player to retrieve the LimboPlayer for
     * @return the player's limbo player, or null if not available
     */
    public LimboPlayer getLimboPlayer(Player player) {
        try {
            return handler.getLimboPlayer(player);
        } catch (Exception e) {
            ConsoleLogger.logException("Could not get LimboPlayer for '" + player.getName() + "'", e);
        }
        return null;
    }

    /**
     * Saves the given LimboPlayer for the provided player.
     *
     * @param player the player to save the LimboPlayer for
     * @param limbo the limbo player to save
     */
    public void saveLimboPlayer(Player player, LimboPlayer limbo) {
        try {
            handler.saveLimboPlayer(player, limbo);
        } catch (Exception e) {
            ConsoleLogger.logException("Could not save LimboPlayer for '" + player.getName() + "'", e);
        }
    }

    /**
     * Removes the LimboPlayer for the given player.
     *
     * @param player the player whose LimboPlayer should be removed
     */
    public void removeLimboPlayer(Player player) {
        try {
            handler.removeLimboPlayer(player);
        } catch (Exception e) {
            ConsoleLogger.logException("Could not remove LimboPlayer for '" + player.getName() + "'", e);
        }
    }

    @Override
    public void reload(Settings settings) {
        LimboPersistenceType persistenceType = settings.getProperty(LimboSettings.LIMBO_PERSISTENCE_TYPE);
        // If we're changing from an existing handler, output a quick hint that nothing is converted.
        if (handler != null && handler.getType() != persistenceType) {
            ConsoleLogger.info("Limbo persistence type has changed! Note that the data is not converted.");
        }
        handler = handlerFactory.newInstance(persistenceType.getImplementationClass());
    }
}
