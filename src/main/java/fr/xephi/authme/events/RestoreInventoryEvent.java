package fr.xephi.authme.events;

import org.bukkit.entity.Player;

/**
 * This event restore the inventory.
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class RestoreInventoryEvent extends CustomEvent {

    private Player player;

    /**
     * Constructor for RestoreInventoryEvent.
     *
     * @param player Player
     */
    public RestoreInventoryEvent(Player player) {
        this.player = player;
    }

    /**
     * Constructor for RestoreInventoryEvent.
     *
     * @param player Player
     * @param async  boolean
     */
    public RestoreInventoryEvent(Player player, boolean async) {
        super(async);
        this.player = player;
    }

    /**
     * Method getPlayer.
     *
     * @return Player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Method setPlayer.
     *
     * @param player Player
     */
    public void setPlayer(Player player) {
        this.player = player;
    }
}
