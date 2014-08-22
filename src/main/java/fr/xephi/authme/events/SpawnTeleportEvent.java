package fr.xephi.authme.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 
 * Called if a player is teleported to a specific spawn
 *
 * @author Xephi59
 */
public class SpawnTeleportEvent extends CustomEvent {

    private Player player;
    private Location to;
    private Location from;
    private boolean isAuthenticated;

    public SpawnTeleportEvent(Player player, Location from, Location to,
            boolean isAuthenticated) {
        this.player = player;
        this.from = from;
        this.to = to;
        this.isAuthenticated = isAuthenticated;
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

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

}
