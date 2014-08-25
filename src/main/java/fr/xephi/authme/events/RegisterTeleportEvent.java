package fr.xephi.authme.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 
 * This event is call if, and only if, a player is teleported just after a
 * register.
 *
 * @author Xephi59
 */
public class RegisterTeleportEvent extends CustomEvent {

    private Player player;
    private Location to;
    private Location from;

    public RegisterTeleportEvent(Player player, Location to) {
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
