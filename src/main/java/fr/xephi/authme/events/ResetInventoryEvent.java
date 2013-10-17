package fr.xephi.authme.events;

import org.bukkit.entity.Player;

/**
*
* @author Xephi59
*/
public class ResetInventoryEvent extends CustomEvent {

	private Player player;

	public ResetInventoryEvent(Player player) {
		this.player = player;
	}

	public Player getPlayer() {
		return this.player;
	}

	public void setPlayer(Player player) {
		this.player = player;
	}

}
