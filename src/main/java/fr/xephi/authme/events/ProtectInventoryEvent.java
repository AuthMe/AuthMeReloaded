package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * 
 * This event is call just after store inventory into cache and will empty the
 * player inventory.
 *
 * @author Xephi59
 */
public class ProtectInventoryEvent extends CustomEvent {

    private ItemStack[] storedinventory;
    private ItemStack[] storedarmor;
    private ItemStack[] emptyInventory = null;
    private ItemStack[] emptyArmor = null;
    private Player player;

    public ProtectInventoryEvent(Player player, ItemStack[] storedinventory,
            ItemStack[] storedarmor) {
        this.player = player;
        this.storedinventory = storedinventory;
        this.storedarmor = storedarmor;
        this.emptyInventory = new ItemStack[36];
        this.emptyArmor = new ItemStack[4];
    }

    public ItemStack[] getStoredInventory() {
        return this.storedinventory;
    }

    public ItemStack[] getStoredArmor() {
        return this.storedarmor;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setNewInventory(ItemStack[] emptyInventory) {
        this.emptyInventory = emptyInventory;
    }

    public ItemStack[] getEmptyInventory() {
        return this.emptyInventory;
    }

    public void setNewArmor(ItemStack[] emptyArmor) {
        this.emptyArmor = emptyArmor;
    }

    public ItemStack[] getEmptyArmor() {
        return this.emptyArmor;
    }

}
