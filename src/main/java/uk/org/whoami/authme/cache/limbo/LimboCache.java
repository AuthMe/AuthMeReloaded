/*
 * Copyright 2011 Sebastian Köhler <sebkoehler@whoami.org.uk>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.org.whoami.authme.cache.limbo;

import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.cache.backup.FileCache;
import uk.org.whoami.authme.events.ResetInventoryEvent;
import uk.org.whoami.authme.events.StoreInventoryEvent;
import uk.org.whoami.authme.settings.Settings;
import uk.org.whoami.authme.settings.Spawn;

public class LimboCache {

    private static LimboCache singleton = null;
    private HashMap<String, LimboPlayer> cache;
    private FileCache playerData = new FileCache();
    public AuthMe plugin;

    private LimboCache(AuthMe plugin) {
    	this.plugin = plugin;
        this.cache = new HashMap<String, LimboPlayer>();
    }

    public void addLimboPlayer(Player player) {
        String name = player.getName().toLowerCase();
        Location loc = player.getLocation();
        int gameMode = player.getGameMode().getValue();
        ItemStack[] arm;
        ItemStack[] inv;
        boolean operator;
        String playerGroup = "";

        if (playerData.doesCacheExist(name)) {
        	StoreInventoryEvent event = new StoreInventoryEvent(player, playerData);
        	Bukkit.getServer().getPluginManager().callEvent(event);
        	if (!event.isCancelled() && event.getInventory() != null && event.getArmor() != null) {
                inv =  event.getInventory();
                arm =  event.getArmor();
        	} else {
        		inv = null;
        		arm = null;
        	}
             playerGroup = playerData.readCache(name).getGroup();
             operator = playerData.readCache(name).getOperator();
        } else {
        	StoreInventoryEvent event = new StoreInventoryEvent(player);
        	Bukkit.getServer().getPluginManager().callEvent(event);
        	if (!event.isCancelled() && event.getInventory() != null && event.getArmor() != null) {
                inv =  event.getInventory();
                arm =  event.getArmor();
        	} else {
        		inv = null;
        		arm = null;
        	}
            if(player.isOp() ) {
                operator = true;
                } else {
                	operator = false;      
                }
        }

        if(Settings.isForceSurvivalModeEnabled) {
            if(Settings.isResetInventoryIfCreative && gameMode != 0 ) {
            	ResetInventoryEvent event = new ResetInventoryEvent(player);
            	Bukkit.getServer().getPluginManager().callEvent(event);
            	if (!event.isCancelled()) {
            		player.sendMessage("Your inventory has been cleaned!");
            	}
            }
            gameMode = 0;
        } 
        if(player.isDead()) {
        	loc = player.getWorld().getSpawnLocation();
        	if (plugin.mv != null)
        		{
        			try {
        				loc = plugin.mv.getMVWorldManager().getMVWorld(player.getWorld().getName()).getSpawnLocation();
        			} catch (NullPointerException npe) {}
        		}
	        if (Spawn.getInstance().getLocation() != null)
	        	loc = Spawn.getInstance().getLocation();
        }
        try {
            if(cache.containsKey(name) && playerGroup.isEmpty()) {
                LimboPlayer groupLimbo = cache.get(name);
                playerGroup = groupLimbo.getGroup();
            }
        } catch (NullPointerException ex) {
        }
        cache.put(player.getName().toLowerCase(), new LimboPlayer(name, loc, inv, arm, gameMode, operator, playerGroup));
    }

    public void addLimboPlayer(Player player, String group) {
        cache.put(player.getName().toLowerCase(), new LimboPlayer(player.getName().toLowerCase(), group));
    }

    public void deleteLimboPlayer(String name) {
        cache.remove(name);
    }

    public LimboPlayer getLimboPlayer(String name) {
        return cache.get(name);
    }

    public boolean hasLimboPlayer(String name) {
        return cache.containsKey(name);
    }

    public static LimboCache getInstance() {
        if (singleton == null) {
            singleton = new LimboCache(AuthMe.getInstance());
        }
        return singleton;
    }

	public void updateLimboPlayer(Player player) {
		if (this.hasLimboPlayer(player.getName().toLowerCase())) {
			this.deleteLimboPlayer(player.getName().toLowerCase());
		}
		this.addLimboPlayer(player);
	}

}
