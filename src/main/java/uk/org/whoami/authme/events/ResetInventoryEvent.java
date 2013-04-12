package uk.org.whoami.authme.events;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import uk.org.whoami.authme.api.API;

/**
*
* @author Xephi59
*/
public class ResetInventoryEvent extends CustomEvent {

	private Player player;

	public ResetInventoryEvent(Player player) {
		this.player = player;
		API.setPlayerInventory(player, new ItemStack[36], new ItemStack[4]);
	}

	public Player getPlayer() {
		return this.player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

}
