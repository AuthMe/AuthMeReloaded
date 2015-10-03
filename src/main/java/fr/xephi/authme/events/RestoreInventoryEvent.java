package fr.xephi.authme.events;

import org.bukkit.entity.Player;

/**
 * This event restore the inventory.
 *
 * @author Xephi59
 */
public class RestoreInventoryEvent extends CustomEvent {

    private Player player;

    public RestoreInventoryEvent(Player player) {
        this.player = player;
    }

    public RestoreInventoryEvent(Player player, boolean async) {
        super(async);
        this.player = player;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }
}
