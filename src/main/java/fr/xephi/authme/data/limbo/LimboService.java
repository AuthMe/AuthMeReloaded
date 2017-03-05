package fr.xephi.authme.data.limbo;

import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Service for managing players that are in "limbo," a temporary state players are
 * put in which have joined but not yet logged in yet.
 */
public class LimboService {

    @Inject
    private LimboCache limboCache;

    LimboService() {
    }


    /**
     * Restores the limbo data and subsequently deletes the entry.
     *
     * @param player the player whose data should be restored
     */
    public void restoreData(Player player) {
        // TODO #1113: Think about architecture for various "restore" strategies
        limboCache.restoreData(player);
        limboCache.deletePlayerData(player);
    }

    /**
     * Returns the limbo player for the given name, or null otherwise.
     *
     * @param name the name to retrieve the data for
     * @return the associated limbo player, or null if none available
     */
    public LimboPlayer getLimboPlayer(String name) {
        return limboCache.getPlayerData(name);
    }

    /**
     * Returns whether there is a limbo player for the given name.
     *
     * @param name the name to check
     * @return true if present, false otherwise
     */
    public boolean hasLimboPlayer(String name) {
        return limboCache.hasPlayerData(name);
    }

    /**
     * Creates a LimboPlayer for the given player.
     *
     * @param player the player to process
     */
    public void createLimboPlayer(Player player) {
        // TODO #1113: We should remove the player's data in here as well
        limboCache.addPlayerData(player);
    }
}
