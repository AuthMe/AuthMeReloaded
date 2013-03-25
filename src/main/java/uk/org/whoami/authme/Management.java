package uk.org.whoami.authme;

import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;

import me.muizers.Notifications.Notification;
import net.md_5.bungee.BungeeCord;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import uk.org.whoami.authme.api.API;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.cache.backup.FileCache;
import uk.org.whoami.authme.cache.limbo.LimboCache;
import uk.org.whoami.authme.cache.limbo.LimboPlayer;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.events.AuthMeTeleportEvent;
import uk.org.whoami.authme.events.LoginEvent;
import uk.org.whoami.authme.events.RestoreInventoryEvent;
import uk.org.whoami.authme.events.SpawnTeleportEvent;
import uk.org.whoami.authme.listener.AuthMePlayerListener;
import uk.org.whoami.authme.security.PasswordSecurity;
import uk.org.whoami.authme.security.RandomString;
import uk.org.whoami.authme.settings.Messages;
import uk.org.whoami.authme.settings.PlayersLogs;
import uk.org.whoami.authme.settings.Settings;

public class Management {

    private Messages m = Messages.getInstance();
    private PlayersLogs pllog = PlayersLogs.getInstance();
    private Utils utils = Utils.getInstance();
    private FileCache playerCache = new FileCache();
    private DataSource database;
    public AuthMe plugin;
    private boolean passpartu = false;
    public static RandomString rdm = new RandomString(Settings.captchaLength);
    public PluginManager pm;
    
    public Management(DataSource database, AuthMe plugin) {
        this.database = database;
        this.plugin = plugin;
        this.pm = plugin.getServer().getPluginManager();
    }

    public Management(DataSource database, boolean passpartu, AuthMe plugin) {
        this.database = database;
        this.passpartu = passpartu;
        this.plugin = plugin;
        this.pm = plugin.getServer().getPluginManager();
    }   
    
    public String performLogin(Player player, String password) {
            
        String name = player.getName().toLowerCase();
        String ip = player.getAddress().getAddress().getHostAddress();
        if (Settings.bungee) {
        	try {
        		ip = BungeeCord.getInstance().getPlayer(player.getName()).getAddress().getAddress().getHostAddress();
        	} catch (NoClassDefFoundError ncdfe) {
        		ConsoleLogger.showError("Your BungeeCord version is outdated, you need a version with the latest API");
        	}
        }
        World world = player.getWorld();
        Location spawnLoc = world.getSpawnLocation();
        if (plugin.mv != null) {
    		try {
    			spawnLoc = plugin.mv.getMVWorldManager().getMVWorld(world).getSpawnLocation();
    		} catch (NullPointerException npe) {
    		} catch (ClassCastException cce) {	
    		} catch (NoClassDefFoundError ncdfe) {
    		}
        }
        
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return m._("logged_in");
           
        }

        if (!database.isAuthAvailable(player.getName().toLowerCase())) {
            return m._("user_unknown");
        }
        
        PlayerAuth pAuth = database.getAuth(name);
            // if Mysql is unavaible
            if(pAuth == null)
                return m._("user_unknown");
            
            //if columnGroup is set
            if(!Settings.getMySQLColumnGroup.isEmpty() && pAuth.getGroupId() == Settings.getNonActivatedGroup) {
            	return m._("vb_nonActiv");
            }
            
        String hash = pAuth.getHash();
        String email = pAuth.getEmail();
        

        try {
            if(!passpartu) {
            	if (Settings.useCaptcha) {
                    if(!plugin.captcha.containsKey(name)) {
                    	plugin.captcha.put(name, 1);
                    } else {
                    	int i = plugin.captcha.get(name) + 1;
                    	plugin.captcha.remove(name);
                    	plugin.captcha.put(name, i);
                    }
                    
                    if(plugin.captcha.containsKey(name) && plugin.captcha.get(name) > Settings.maxLoginTry) {
                    	player.sendMessage(m._("need_captcha"));
                    	plugin.cap.put(name, rdm.nextString());
                    	return "Type : /captcha " + plugin.cap.get(name);
                    } else if (plugin.captcha.containsKey(name) && plugin.captcha.get(name) > Settings.maxLoginTry) {
                    	try {
                    		plugin.captcha.remove(name);
                    		plugin.cap.remove(name);
                    	} catch (NullPointerException npe) {
                    	}

                    }
            	}
            if (PasswordSecurity.comparePasswordWithHash(password, hash, name)) {
                PlayerAuth auth = new PlayerAuth(name, hash, ip, new Date().getTime(), email);
            
                database.updateSession(auth);
                PlayerCache.getInstance().addPlayer(auth);
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
                if (limbo != null) {

                	
                      player.setOp(limbo.getOperator());
                    
                      this.utils.addNormal(player, limbo.getGroup());
                    
                    
                      if ((Settings.isTeleportToSpawnEnabled.booleanValue()) && (!Settings.isForceSpawnLocOnJoinEnabled.booleanValue()  && Settings.getForcedWorlds.contains(player.getWorld().getName())))
                                {
                        if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0))
                                  {
                          this.utils.packCoords(this.database.getAuth(name).getQuitLocX(), this.database.getAuth(name).getQuitLocY(), this.database.getAuth(name).getQuitLocZ(), player);
                                  }
                                  else {

                          AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
                          pm.callEvent(tpEvent);
                          if(!tpEvent.isCancelled()) {
                          	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                        		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                        	}
                        	  player.teleport(tpEvent.getTo());
                          }
                         
                                  }
                    
                                }
                      else if (Settings.isForceSpawnLocOnJoinEnabled.booleanValue() && Settings.getForcedWorlds.contains(player.getWorld().getName())) {
                          SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, true);
                          pm.callEvent(tpEvent);
                          if(!tpEvent.isCancelled()) {
                          	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                        		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                        	}
                        	  player.teleport(tpEvent.getTo());
                          }
                                }
                      else if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0))
                                {
                        this.utils.packCoords(this.database.getAuth(name).getQuitLocX(), this.database.getAuth(name).getQuitLocY(), this.database.getAuth(name).getQuitLocZ(), player);
                                }
                                else {
                        AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
                        pm.callEvent(tpEvent);
                        if(!tpEvent.isCancelled()) {
                        	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                        		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                        	}
                      	  player.teleport(tpEvent.getTo());
                        }
                                }
                      
                      
                      player.setGameMode(GameMode.getByValue(limbo.getGameMode()));
                      
                      if (Settings.protectInventoryBeforeLogInEnabled.booleanValue() && player.hasPlayedBefore()) {
                      		RestoreInventoryEvent event = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                      		Bukkit.getServer().getPluginManager().callEvent(event);
                      		if (!event.isCancelled()) {
                      			API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
                      		}
                      }

                    
                      player.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
                      player.getServer().getScheduler().cancelTask(limbo.getMessageTaskId());
                      LimboCache.getInstance().deleteLimboPlayer(name);
                      if (this.playerCache.doesCacheExist(name)) {
                        this.playerCache.removeCache(name);
                                }
                    
                              }
                
               /*
                *  Little Work Around under Registration Group Switching for admins that
                *  add Registration thru a web Scripts.
                */
                if ( Settings.isPermissionCheckEnabled && AuthMe.permission.playerInGroup(player, Settings.unRegisteredGroup) && !Settings.unRegisteredGroup.isEmpty() ) {
                    AuthMe.permission.playerRemoveGroup(player.getWorld(), player.getName(), Settings.unRegisteredGroup);
                    AuthMe.permission.playerAddGroup(player.getWorld(), player.getName(), Settings.getRegisteredGroup);
                }
                
                try {
                    if (!PlayersLogs.players.contains(player.getName()))
                    	PlayersLogs.players.add(player.getName());
                    pllog.save();
                } catch (NullPointerException ex) {
                	
                }
                
                Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
                if (Settings.useCaptcha) {
                    if(plugin.captcha.containsKey(name)) {
                    	plugin.captcha.remove(name);
                    }
                    if(plugin.cap.containsKey(name)) {
                    	plugin.cap.containsKey(name);
                    }
                }
                player.sendMessage(m._("login"));
                displayOtherAccounts(auth);
                if(!Settings.noConsoleSpam)
                ConsoleLogger.info(player.getDisplayName() + " logged in!");
                if(plugin.notifications != null) {
                	plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " logged in!"));
                }
                player.saveData();
                
            } else {
            	if (!Settings.noConsoleSpam)
                ConsoleLogger.info(player.getDisplayName() + " used the wrong password");
                if (Settings.isKickOnWrongPasswordEnabled) {
                    int gm = AuthMePlayerListener.gameMode.get(name);
                	player.setGameMode(GameMode.getByValue(gm));
                    player.kickPlayer(m._("wrong_pwd"));
                } else {
                    return (m._("wrong_pwd"));
                }
            }
         } else {
            // need for bypass password check if passpartu command is enabled
                PlayerAuth auth = new PlayerAuth(name, hash, ip, new Date().getTime(), email);
                database.updateSession(auth);
                PlayerCache.getInstance().addPlayer(auth);
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
                if (limbo != null) {

                      
                      player.setOp(limbo.getOperator());
                      
                      this.utils.addNormal(player, limbo.getGroup());
                      

                      if ((Settings.isTeleportToSpawnEnabled.booleanValue()) && (!Settings.isForceSpawnLocOnJoinEnabled.booleanValue() && Settings.getForcedWorlds.contains(player.getWorld().getName())))
                                {
                        if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0)) {
                          Location quitLoc = new Location(player.getWorld(), this.database.getAuth(name).getQuitLocX() + 0.5D, this.database.getAuth(name).getQuitLocY() + 0.5D, this.database.getAuth(name).getQuitLocZ() + 0.5D);
                          AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, quitLoc);
                          pm.callEvent(tpEvent);
                          if(!tpEvent.isCancelled()) {
                          	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                        		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                        	}
                        	  player.teleport(tpEvent.getTo());
                          }
                                  }
                                  else
                                  {
                          AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
                          pm.callEvent(tpEvent);
                          if(!tpEvent.isCancelled()) {
                          	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                        		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                        	}
                        	  player.teleport(tpEvent.getTo());
                          }
                                  }
                      
                                }
                      else if (Settings.isForceSpawnLocOnJoinEnabled.booleanValue() && Settings.getForcedWorlds.contains(player.getWorld().getName())) {

                          SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, true);
                          pm.callEvent(tpEvent);
                          if(!tpEvent.isCancelled()) {
                          	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                        		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                        	}
                        	  player.teleport(tpEvent.getTo());
                          }
                                }
                      else if ((Settings.isSaveQuitLocationEnabled.booleanValue()) && (this.database.getAuth(name).getQuitLocY() != 0)) {
                        Location quitLoc = new Location(player.getWorld(), this.database.getAuth(name).getQuitLocX() + 0.5D, this.database.getAuth(name).getQuitLocY() + 0.5D, this.database.getAuth(name).getQuitLocZ() + 0.5D);
                        AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, quitLoc);
                        pm.callEvent(tpEvent);
                        if(!tpEvent.isCancelled()) {
                        	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                        		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                        	}
                      	  player.teleport(tpEvent.getTo());
                        }
                                }
                                else
                                {
                        AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
                        pm.callEvent(tpEvent);
                        if(!tpEvent.isCancelled()) {
                        	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                        		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                        	}
                      	  player.teleport(tpEvent.getTo());
                        }
                                }
                      
                      
                      player.setGameMode(GameMode.getByValue(limbo.getGameMode()));
                      
                      if (Settings.protectInventoryBeforeLogInEnabled.booleanValue() && player.hasPlayedBefore()) {
                      	RestoreInventoryEvent event = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                      	Bukkit.getServer().getPluginManager().callEvent(event);
                      	if (!event.isCancelled()) {
                      		API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
                      	}
                      }
                      
                      
                      player.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
                      player.getServer().getScheduler().cancelTask(limbo.getMessageTaskId());
                      LimboCache.getInstance().deleteLimboPlayer(name);
                      if (this.playerCache.doesCacheExist(name)) {
                        this.playerCache.removeCache(name);
                                }
                              }
                
               /*
                *  Little Work Around under Registration Group Switching for admins that
                *  add Registration thru a web Scripts.
                */
                if ( Settings.isPermissionCheckEnabled && AuthMe.permission.playerInGroup(player, Settings.unRegisteredGroup) && !Settings.unRegisteredGroup.isEmpty() ) {
                    AuthMe.permission.playerRemoveGroup(player.getWorld(), player.getName(), Settings.unRegisteredGroup);
                    AuthMe.permission.playerAddGroup(player.getWorld(), player.getName(), Settings.getRegisteredGroup);
                }
                
                try {
                    if (!PlayersLogs.players.contains(player.getName()))
                    	PlayersLogs.players.add(player.getName());
                    pllog.save();
                } catch (NullPointerException ex) { }
                
                Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
                if (Settings.useCaptcha) {
                    if(plugin.captcha.containsKey(name)) {
                    	plugin.captcha.remove(name);
                    }
                    if(plugin.cap.containsKey(name)) {
                    	plugin.cap.containsKey(name);
                    }
                }
                player.sendMessage(m._("login"));
                displayOtherAccounts(auth);
                if(!Settings.noConsoleSpam)
                ConsoleLogger.info(player.getDisplayName() + " logged in!");
                if(plugin.notifications != null) {
                	plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " logged in!"));
                }
                player.saveData(); 
                this.passpartu = false;
            }                
          
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            return (m._("error"));
        }
        return "";
	}
    
    private void displayOtherAccounts(PlayerAuth auth) {
    	if (!Settings.displayOtherAccounts) {
    		return;
    	}
    	if (auth == null) {
    		return;
    	}
    	if (this.database.getAllAuthsByName(auth).isEmpty() || this.database.getAllAuthsByName(auth) == null) {
    		return;
    	}
    	if(this.database.getAllAuthsByName(auth).size() == 1) {
    		return;
    	}
    	List<String> accountList = this.database.getAllAuthsByName(auth);
    	String message = "[AuthMe] ";
    	int i = 0;
    	for (String account : accountList) {
    		i++;
    		message = message + account;
    		if (i != accountList.size()) {
    			message = message + ", ";
    		} else {
    			message = message + ".";
    		}
    		
    	}
    	for (Player player : AuthMe.getInstance().getServer().getOnlinePlayers()) {
    		if (player.hasPermission("authme.seeOtherAccounts")) {
    			player.sendMessage("[AuthMe] The player " + auth.getNickname() + " has " + String.valueOf(accountList.size()) + " accounts");
    			player.sendMessage(message);
    		}
    	}
    }
    
    
}
