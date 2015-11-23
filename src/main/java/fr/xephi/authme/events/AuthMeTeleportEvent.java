package fr.xephi.authme.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * This event is call when AuthMe try to teleport a player
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class AuthMeTeleportEvent extends CustomEvent {

    private Player player;
    private Location to;
    private Location from;

    /**
     * Constructor for AuthMeTeleportEvent.
     *
     * @param player Player
     * @param to     Location
     */
    public AuthMeTeleportEvent(Player player, Location to) {
        this.player = player;
        this.from = player.getLocation();
        this.to = to;
    }

    /**
     * Method getPlayer.
     *
     * @return Player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Method getTo.
     *
     * @return Location
     */
    public Location getTo() {
        return to;
    }

    /**
     * Method setTo.
     *
     * @param to Location
     */
    public void setTo(Location to) {
        this.to = to;
    }

    /**
     * Method getFrom.
     *
     * @return Location
     */
    public Location getFrom() {
        return from;
    }

}
