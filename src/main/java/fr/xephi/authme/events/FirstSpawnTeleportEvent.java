package fr.xephi.authme.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 
 * Called if a player is teleported to the authme first spawn
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class FirstSpawnTeleportEvent extends CustomEvent {

    private Player player;
    private Location to;
    private Location from;

    /**
     * Constructor for FirstSpawnTeleportEvent.
     * @param player Player
     * @param from Location
     * @param to Location
     */
    public FirstSpawnTeleportEvent(Player player, Location from, Location to) {
        super(true);
        this.player = player;
        this.from = from;
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
