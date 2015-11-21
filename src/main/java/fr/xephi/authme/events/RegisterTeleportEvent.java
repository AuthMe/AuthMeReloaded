package fr.xephi.authme.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 
 * This event is call if, and only if, a player is teleported just after a
 * register.
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class RegisterTeleportEvent extends CustomEvent {

    private Player player;
    private Location to;
    private Location from;

    /**
     * Constructor for RegisterTeleportEvent.
     * @param player Player
     * @param to Location
     */
    public RegisterTeleportEvent(Player player, Location to) {
        this.player = player;
        this.from = player.getLocation();
        this.to = to;
    }

    /**
     * Method getPlayer.
     * @return Player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Method setTo.
     * @param to Location
     */
    public void setTo(Location to) {
        this.to = to;
    }

    /**
     * Method getTo.
     * @return Location
     */
    public Location getTo() {
        return to;
    }

    /**
     * Method getFrom.
     * @return Location
     */
    public Location getFrom() {
        return from;
    }

}
