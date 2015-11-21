package fr.xephi.authme.events;

import org.bukkit.entity.Player;

/**
 * 
 * This event is call when a creative inventory is reseted.
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class ResetInventoryEvent extends CustomEvent {

    private Player player;

    /**
     * Constructor for ResetInventoryEvent.
     * @param player Player
     */
    public ResetInventoryEvent(Player player) {
        super(true);
        this.player = player;
    }

    /**
     * Method getPlayer.
     * @return Player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Method setPlayer.
     * @param player Player
     */
    public void setPlayer(Player player) {
        this.player = player;
    }

}
