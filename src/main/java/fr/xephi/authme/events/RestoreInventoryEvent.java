package fr.xephi.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 *
 * This event restore the inventory from cache
 *
 * @author Xephi59
 */
public class RestoreInventoryEvent extends CustomEvent {

    private ItemStack[] inventory;
    private ItemStack[] armor;
    private Player player;

    public RestoreInventoryEvent(Player player, ItemStack[] inventory,
            ItemStack[] armor) {
        this.player = player;
        this.inventory = inventory;
        this.armor = armor;
    }

    public ItemStack[] getInventory() {
        return this.inventory;
    }

    public void setInventory(ItemStack[] inventory) {
        this.inventory = inventory;
    }

    public ItemStack[] getArmor() {
        return this.armor;
    }

    public void setArmor(ItemStack[] armor) {
        this.armor = armor;
    }

    public Player getPlayer() {
        return this.player;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

}
