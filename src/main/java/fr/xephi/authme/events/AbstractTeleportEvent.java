package fr.xephi.authme.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

/**
 * Common supertype for all AuthMe teleport events.
 */
public abstract class AbstractTeleportEvent extends CustomEvent implements Cancellable {

    private final Player player;
    private final Location from;
    private Location to;
    private boolean isCancelled;

    /**
     * Constructor.
     *
     * @param isAsync Whether to fire the event asynchronously or not
     * @param player The player
     * @param to The teleport destination
     */
    public AbstractTeleportEvent(boolean isAsync, Player player, Location to) {
        super(isAsync);
        this.player = player;
        this.from = player.getLocation();
        this.to = to;
    }

    /**
     * Return the player planned to be teleported.
     *
     * @return The player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Return the location the player is being teleported away from.
     *
     * @return The location prior to the teleport
     */
    public Location getFrom() {
        return from;
    }

    /**
     * Set the destination of the teleport.
     *
     * @param to The location to teleport the player to
     */
    public void setTo(Location to) {
        this.to = to;
    }

    /**
     * Return the destination the player is being teleported to.
     *
     * @return The teleport destination
     */
    public Location getTo() {
        return to;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }


}
