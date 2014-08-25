package fr.xephi.authme.plugin.manager;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.trc202.CombatTag.CombatTag;
import com.trc202.CombatTagApi.CombatTagApi;

public abstract class CombatTagComunicator {

    public static CombatTagApi combatApi;

    /**
     * Returns if the entity is an NPC
     * 
     * @param player
     * @return true if the player is an NPC
     */
    public static boolean isNPC(Entity player) {
        try {
            if (Bukkit.getServer().getPluginManager().getPlugin("CombatTag") != null) {
                combatApi = new CombatTagApi((CombatTag) Bukkit.getServer().getPluginManager().getPlugin("CombatTag"));
                try {
                    combatApi.getClass().getMethod("isNPC");
                } catch (Exception e) {
                    return false;
                }
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
