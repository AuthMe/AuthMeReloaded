package fr.xephi.authme.events;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

/**
 * Event fired after a player has been unregistered from an external source (by an admin or via the API).
 * <p>
 * Note that only the {@code playerName} is guaranteed to be not {@code null} in any case.
 * <p>
 * The {@code player} may be null if a name is supplied which has never been online on the server &ndash;
 * due to migrations, data removal, etc. it is possible that a user exists in the database for which the
 * server knows no {@link Player} object.
 * <p>
 * If a player is unregistered via an API call, the {@code initiator} is null as the action has not been
 * started by any {@link CommandSender}. Otherwise, the {@code initiator} is the user who performed the
 * command to unregister the player name.
 */
public class UnregisterByAdminEvent extends AbstractUnregisterEvent {

    private static final HandlerList handlers = new HandlerList();
    private final String playerName;
    private final CommandSender initiator;

    /**
     * Constructor.
     *
     * @param player the player (may be null - see class JavaDoc)
     * @param playerName the name of the player that was unregistered
     * @param isAsync whether or not the event is async
     * @param initiator the initiator of the unregister process (may be null - see class JavaDoc)
     */
    public UnregisterByAdminEvent(Player player, String playerName, boolean isAsync, CommandSender initiator) {
        super(player, isAsync);
        this.playerName = playerName;
        this.initiator = initiator;
    }

    /**
     * @return the name of the player that was unregistered
     */
    public String getPlayerName() {
        return playerName;
    }

    /**
     * @return the user who requested to unregister the name, or null if not applicable
     */
    public CommandSender getInitiator() {
        return initiator;
    }

    /**
     * Return the list of handlers, equivalent to {@link #getHandlers()} and required by {@link org.bukkit.event.Event}.
     *
     * @return The list of handlers
     */
    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}
