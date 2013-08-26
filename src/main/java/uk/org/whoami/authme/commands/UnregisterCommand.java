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

package uk.org.whoami.authme.commands;

import java.security.NoSuchAlgorithmException;

import me.muizers.Notifications.Notification;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.ConsoleLogger;
import uk.org.whoami.authme.Utils;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.cache.backup.FileCache;
import uk.org.whoami.authme.cache.limbo.LimboCache;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.events.SpawnTeleportEvent;
import uk.org.whoami.authme.security.PasswordSecurity;
import uk.org.whoami.authme.settings.Messages;
import uk.org.whoami.authme.settings.PlayersLogs;
import uk.org.whoami.authme.settings.Settings;
import uk.org.whoami.authme.settings.Spawn;
import uk.org.whoami.authme.task.MessageTask;
import uk.org.whoami.authme.task.TimeoutTask;

public class UnregisterCommand implements CommandExecutor {

    private Messages m = Messages.getInstance();
    private PlayersLogs pllog = PlayersLogs.getInstance();
    public AuthMe plugin;
    private DataSource database;
    private FileCache playerCache = new FileCache();

    public UnregisterCommand(AuthMe plugin, DataSource database) {
        this.plugin = plugin;
        this.database = database;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmnd, String label, String[] args) {
        if (!(sender instanceof Player)) {
            return true;
        }

        if (!plugin.authmePermissible(sender, "authme." + label.toLowerCase())) {
            sender.sendMessage(m._("no_perm"));
            return true;
        }

        Player player = (Player) sender;
        String name = player.getName().toLowerCase();

        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            player.sendMessage(m._("not_logged_in"));
            return true;
        }

        if (args.length != 1) {
            player.sendMessage(m._("usage_unreg"));
            return true;
        }
        try {
            if (PasswordSecurity.comparePasswordWithHash(args[0], PlayerCache.getInstance().getAuth(name).getHash(), name)) {
                if (!database.removeAuth(name)) {
                    player.sendMessage("error");
                    return true;
                }
                if(Settings.isForcedRegistrationEnabled) {
                    player.getInventory().setArmorContents(new ItemStack[4]);
                    player.getInventory().setContents(new ItemStack[36]); 
                    player.saveData();
                    PlayerCache.getInstance().removePlayer(player.getName().toLowerCase());
                    LimboCache.getInstance().addLimboPlayer(player);
                    int delay = Settings.getRegistrationTimeout * 20;
                    int interval = Settings.getWarnMessageInterval;
                    BukkitScheduler sched = sender.getServer().getScheduler();
                    if (delay != 0) {
                        BukkitTask id = sched.runTaskLater(plugin, new TimeoutTask(plugin, name), delay);
                        LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id.getTaskId());
                    }
                    sched.scheduleSyncDelayedTask(plugin, new MessageTask(plugin, name, m._("reg_msg"), interval));
                        if(!Settings.unRegisteredGroup.isEmpty()){
                            Utils.getInstance().setGroup(player, Utils.groupType.UNREGISTERED);
                        }
                        player.sendMessage("unregistered");
                        ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                        if(plugin.notifications != null) {
                        	plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " unregistered himself!"));
                        }
                    return true;
                }
                if(!Settings.unRegisteredGroup.isEmpty()){
                     Utils.getInstance().setGroup(player, Utils.groupType.UNREGISTERED);
                  }
                 PlayerCache.getInstance().removePlayer(player.getName().toLowerCase());
                // check if Player cache File Exist and delete it, preventing duplication of items
                 if(playerCache.doesCacheExist(name)) {
                        playerCache.removeCache(name);
                 }
                 if (PlayersLogs.players.contains(player.getName())) {
                	 PlayersLogs.players.remove(player.getName());
                	 pllog.save();
                 }
                 player.sendMessage("unregistered");
                 ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                 if(plugin.notifications != null) {
                 	plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " unregistered himself!"));
                 }
                 if (Settings.isTeleportToSpawnEnabled) {
                	 Location spawn = player.getWorld().getSpawnLocation();
                     if (plugin.mv != null) {
                 		try {
                 			spawn = plugin.mv.getMVWorldManager().getMVWorld(player.getWorld()).getSpawnLocation();
                 		} catch (NullPointerException npe) {
                 		} catch (ClassCastException cce) {	
                 		} catch (NoClassDefFoundError ncdfe) {
                 		}
                     }
                     if (plugin.essentialsSpawn != null) {
                     	spawn = plugin.essentialsSpawn;
                     }
                     if (Spawn.getInstance().getLocation() != null)
                     	spawn = Spawn.getInstance().getLocation();
                     SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawn, false);
                     plugin.getServer().getPluginManager().callEvent(tpEvent);
                     if(!tpEvent.isCancelled()) {
                     	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                   		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                   	}
                   	  player.teleport(tpEvent.getTo());
                     }
                 }
                return true;
            } else {
                player.sendMessage(m._("wrong_pwd"));
            }
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            sender.sendMessage("Internal Error please read the server log");
        }
        return true;
    }
}
