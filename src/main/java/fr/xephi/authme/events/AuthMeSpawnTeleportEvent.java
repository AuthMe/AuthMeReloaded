package fr.xephi.authme.events;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Location;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called if a player is teleported to a specific spawn upon joining or logging in.
 */
public class AuthMeSpawnTeleportEvent extends CustomEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();

    @NotNull
    private final PlayerProfile profile;
    @NotNull
    private final Location from;
    @NotNull
    private Location to;
    private final boolean isAuthenticated;
    private boolean isCancelled;

    /**
     * Constructor.
     *
     * @param profile The player profile
     * @param from The teleport origin
     * @param to The teleport destination
     * @param isAuthenticated Whether the player is logged in
     */
    public AuthMeSpawnTeleportEvent(@NotNull PlayerProfile profile, @NotNull Location from, @NotNull Location to, boolean isAuthenticated) {
        super(false);
        this.profile = profile;
        this.from = from;
        this.to = to;
        this.isAuthenticated = isAuthenticated;
    }

    /**
     * Return the player planned to be teleported.
     *
     * @return The player profile
     */
    @NotNull
    public PlayerProfile getProfile() {
        return profile;
    }

    /**
     * Return the location the player is being teleported away from.
     *
     * @return The location prior to the teleport
     */
    @NotNull
    public Location getFrom() {
        return from;
    }

    /**
     * Set the destination of the teleport.
     *
     * @param to The location to teleport the player to
     */
    public void setTo(@NotNull Location to) {
        this.to = to;
    }

    /**
     * Return the destination the player is being teleported to.
     *
     * @return The teleport destination
     */
    @NotNull
    public Location getTo() {
        return to;
    }

    /**
     * Return whether the player is authenticated.
     *
     * @return true if the player is logged in, false otherwise
     */
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.isCancelled = cancel;
    }

    /**
     * Return the list of handlers, equivalent to {@link #getHandlers()} and required by {@link Event}.
     *
     * @return The list of handlers
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }
}
