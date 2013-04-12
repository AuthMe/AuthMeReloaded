package uk.org.whoami.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import uk.org.whoami.authme.api.API;

/**
*
* @author Xephi59
*/
public class ProtectInventoryEvent extends CustomEvent {

	private ItemStack[] storedinventory;
	private ItemStack[] storedarmor;
	private ItemStack[] emptyInventory = null;
	private ItemStack[] emptyArmor = null;
	private Player player;

	public ProtectInventoryEvent(Player player, ItemStack[] storedinventory, ItemStack[] storedarmor) {
		this.player = player;
		this.storedinventory = storedinventory;
		this.storedarmor = storedarmor;
	}

	public ProtectInventoryEvent(Player player, ItemStack[] storedinventory, ItemStack[] storedarmor, int newInventory, int newArmor) {
		this.player = player;
		this.storedinventory = storedinventory;
		this.storedarmor = storedarmor;
		this.setNewInventory(new ItemStack[newInventory]);
		this.setNewArmor(new ItemStack[newArmor]);
		API.setPlayerInventory(player, new ItemStack[newInventory], new ItemStack[newArmor]);
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
