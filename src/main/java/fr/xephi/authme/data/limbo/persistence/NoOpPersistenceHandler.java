package fr.xephi.authme.data.limbo.persistence;

import fr.xephi.authme.data.limbo.LimboPlayer;
import org.bukkit.entity.Player;

/**
 * Limbo player persistence implementation that does nothing.
 */
class NoOpPersistenceHandler implements LimboPersistenceHandler {

    @Override
    public LimboPlayer getLimboPlayer(Player player) {
        return null;
    }

    @Override
    public void saveLimboPlayer(Player player, LimboPlayer limbo) {
        // noop
    }

    @Override
    public void removeLimboPlayer(Player player) {
        // noop
    }

    @Override
    public LimboPersistenceType getType() {
        return LimboPersistenceType.DISABLED;
    }
}
