package fr.xephi.authme.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 
 * Called if a player is teleported to the authme first spawn
 *
 * @author Xephi59
 */
public class FirstSpawnTeleportEvent extends CustomEvent {

    private Player player;
    private Location to;
    private Location from;

    public FirstSpawnTeleportEvent(Player player, Location from, Location to) {
        this.player = player;
        this.from = from;
        this.to = to;
    }

    public Player getPlayer() {
        return player;
    }

    public void setTo(Location to) {
        this.to = to;
    }

    public Location getTo() {
        return to;
    }

    public Location getFrom() {
        return from;
    }

}
