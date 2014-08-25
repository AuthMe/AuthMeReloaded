package fr.xephi.authme.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 *
 * This event is call when AuthMe try to teleport a player
 *
 * @author Xephi59
 */
public class AuthMeTeleportEvent extends CustomEvent {

    private Player player;
    private Location to;
    private Location from;

    public AuthMeTeleportEvent(Player player, Location to) {
        this.player = player;
        this.from = player.getLocation();
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
