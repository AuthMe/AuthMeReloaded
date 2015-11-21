package fr.xephi.authme.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * 
 * Called if a player is teleported to a specific spawn
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class SpawnTeleportEvent extends CustomEvent {

    private Player player;
    private Location to;
    private Location from;
    private boolean isAuthenticated;

    /**
     * Constructor for SpawnTeleportEvent.
     * @param player Player
     * @param from Location
     * @param to Location
     * @param isAuthenticated boolean
     */
    public SpawnTeleportEvent(Player player, Location from, Location to,
            boolean isAuthenticated) {
        this.player = player;
        this.from = from;
        this.to = to;
        this.isAuthenticated = isAuthenticated;
    }

    /**
     * Method getPlayer.
    
     * @return Player */
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
    
     * @return Location */
    public Location getTo() {
        return to;
    }

    /**
     * Method getFrom.
    
     * @return Location */
    public Location getFrom() {
        return from;
    }

    /**
     * Method isAuthenticated.
    
     * @return boolean */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

}
