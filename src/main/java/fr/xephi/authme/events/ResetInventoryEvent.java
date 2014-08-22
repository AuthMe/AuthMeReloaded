package fr.xephi.authme.events;

import org.bukkit.entity.Player;

/**
 * 
 * This event is call when a creative inventory is reseted.
 *
 * @author Xephi59
 */
public class ResetInventoryEvent extends CustomEvent {

    private Player player;

    public ResetInventoryEvent(Player player) {
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

}
