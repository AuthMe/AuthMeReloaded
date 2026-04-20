package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

/**
 * This event is called before the inventory data of a player is suppressed,
 * i.e. the inventory of the player is not displayed until he has authenticated.
 */
public class ProtectInventoryEvent extends CustomEvent implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final ItemStack[] storedInventory;
    private final ItemStack[] storedArmor;
    private final Player player;
    private boolean isCancelled;

    /**
     * Constructor.
     *
     * @param player The player
     * @param isAsync True if the event is async, false otherwise
     */
    public ProtectInventoryEvent(Player player, boolean isAsync) {
        super(isAsync);
        this.player = player;
        this.storedInventory = player.getInventory().getContents();
        this.storedArmor = player.getInventory().getArmorContents();
    }

    /**
     * Return the inventory of the player.
     *
     * @return The player's inventory
     */
    public ItemStack[] getStoredInventory() {
        return storedInventory;
    }

    /**
     * Return the armor of the player.
     *
     * @return The player's armor
     */
    public ItemStack[] getStoredArmor() {
        return storedArmor;
    }

    /**
     * Return the player whose inventory will be hidden.
     *
     * @return The player associated with this event
     */
    public Player getPlayer() {
        return player;
    }

    @Override
    public void setCancelled(boolean isCancelled) {
        this.isCancelled = isCancelled;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
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
    public HandlerList getHandlers() {
        return handlers;
    }

}
