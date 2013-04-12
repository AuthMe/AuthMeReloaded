/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.org.whoami.authme.plugin.manager;

import com.trc202.CombatTag.CombatTag;
import com.trc202.CombatTagApi.CombatTagApi;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/**
 *
 * @author stefano
 */
public abstract class CombatTagComunicator {

	static CombatTagApi combatApi;

	public CombatTagComunicator() {
        if(Bukkit.getServer().getPluginManager().getPlugin("CombatTag") != null){
			combatApi = new CombatTagApi((CombatTag)Bukkit.getServer().getPluginManager().getPlugin("CombatTag")); 
		}
    }
	/**
	 * Checks to see if the player is in combat. The combat time can be configured by the server owner
	 * If the player has died while in combat the player is no longer considered in combat and as such will return false
	 * @param playerName
	 * @return true if player is in combat
	 */
	public abstract boolean isInCombat(String player);

	/**
	 * Checks to see if the player is in combat. The combat time can be configured by the server owner
	 * If the player has died while in combat the player is no longer considered in combat and as such will return false
	 * @param player
	 * @return true if player is in combat
	 */
	public abstract boolean isInCombat(Player player);
	/**
	 * Returns the time before the tag is over
	 *  -1 if the tag has expired
	 *  -2 if the player is not in combat
	 * @param player
	 * @return
	 */
	public abstract long getRemainingTagTime(String player);

	/**
	 * Returns if the entity is an NPC
	 * @param player
	 * @return true if the player is an NPC
	 */
	public static boolean isNPC(Entity player) {
		try {
			if(Bukkit.getServer().getPluginManager().getPlugin("CombatTag") != null){
				combatApi = new CombatTagApi((CombatTag) Bukkit.getServer().getPluginManager().getPlugin("CombatTag"));
				return combatApi.isNPC(player);
			}
		} catch (ClassCastException ex) {
			return false;
		} catch (NullPointerException npe) {
			return false;
		} catch (NoClassDefFoundError ncdfe) {
			return false;
		}
		return false;
	}

}
