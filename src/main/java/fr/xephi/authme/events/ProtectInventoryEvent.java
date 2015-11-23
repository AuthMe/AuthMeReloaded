package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * This event is call just after store inventory into cache and will empty the
 * player inventory.
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class ProtectInventoryEvent extends CustomEvent {

    private final ItemStack[] storedinventory;
    private final ItemStack[] storedarmor;
    private ItemStack[] emptyInventory = null;
    private ItemStack[] emptyArmor = null;
    private final Player player;

    /**
     * Constructor for ProtectInventoryEvent.
     *
     * @param player Player
     */
    public ProtectInventoryEvent(Player player) {
        super(true);
        this.player = player;
        this.storedinventory = player.getInventory().getContents();
        this.storedarmor = player.getInventory().getArmorContents();
        this.emptyInventory = new ItemStack[36];
        this.emptyArmor = new ItemStack[4];
    }

    /**
     * Method getStoredInventory.
     *
     * @return ItemStack[]
     */
    public ItemStack[] getStoredInventory() {
        return this.storedinventory;
    }

    /**
     * Method getStoredArmor.
     *
     * @return ItemStack[]
     */
    public ItemStack[] getStoredArmor() {
        return this.storedarmor;
    }

    /**
     * Method getPlayer.
     *
     * @return Player
     */
    public Player getPlayer() {
        return this.player;
    }

    /**
     * Method setNewInventory.
     *
     * @param emptyInventory ItemStack[]
     */
    public void setNewInventory(ItemStack[] emptyInventory) {
        this.emptyInventory = emptyInventory;
    }

    /**
     * Method getEmptyInventory.
     *
     * @return ItemStack[]
     */
    public ItemStack[] getEmptyInventory() {
        return this.emptyInventory;
    }

    /**
     * Method setNewArmor.
     *
     * @param emptyArmor ItemStack[]
     */
    public void setNewArmor(ItemStack[] emptyArmor) {
        this.emptyArmor = emptyArmor;
    }

    /**
     * Method getEmptyArmor.
     *
     * @return ItemStack[]
     */
    public ItemStack[] getEmptyArmor() {
        return this.emptyArmor;
    }

}
