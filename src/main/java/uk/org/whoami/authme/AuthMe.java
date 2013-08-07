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

package uk.org.whoami.authme;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import com.earth2me.essentials.Essentials;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import uk.org.whoami.authme.api.API;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.cache.backup.FileCache;
import uk.org.whoami.authme.cache.limbo.LimboCache;
import uk.org.whoami.authme.cache.limbo.LimboPlayer;
import uk.org.whoami.authme.commands.AdminCommand;
import uk.org.whoami.authme.commands.CaptchaCommand;
import uk.org.whoami.authme.commands.ChangePasswordCommand;
import uk.org.whoami.authme.commands.EmailCommand;
import uk.org.whoami.authme.commands.LoginCommand;
import uk.org.whoami.authme.commands.LogoutCommand;
import uk.org.whoami.authme.commands.RegisterCommand;
import uk.org.whoami.authme.commands.UnregisterCommand;
import uk.org.whoami.authme.datasource.CacheDataSource;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.datasource.FileDataSource;
import uk.org.whoami.authme.datasource.MiniConnectionPoolManager.TimeoutException;
import uk.org.whoami.authme.datasource.MySQLDataSource;
import uk.org.whoami.authme.listener.AuthMeBlockListener;
import uk.org.whoami.authme.listener.AuthMeChestShopListener;
import uk.org.whoami.authme.listener.AuthMeEntityListener;
import uk.org.whoami.authme.listener.AuthMePlayerListener;
import uk.org.whoami.authme.listener.AuthMeSpoutListener;
import uk.org.whoami.authme.plugin.manager.BungeeCordMessage;
import uk.org.whoami.authme.plugin.manager.CitizensCommunicator;
import uk.org.whoami.authme.plugin.manager.CombatTagComunicator;
import uk.org.whoami.authme.plugin.manager.EssSpawn;
import uk.org.whoami.authme.settings.Messages;
import uk.org.whoami.authme.settings.PlayersLogs;
import uk.org.whoami.authme.settings.Settings;
import uk.org.whoami.authme.threads.FlatFileThread;
import uk.org.whoami.authme.threads.MySQLThread;
import uk.org.whoami.authme.threads.SQLiteThread;

import me.muizers.Notifications.Notifications;
import net.citizensnpcs.Citizens;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;

import com.onarandombox.MultiverseCore.MultiverseCore;

import uk.org.whoami.authme.commands.PasspartuCommand;
import uk.org.whoami.authme.datasource.SqliteDataSource;

public class AuthMe extends JavaPlugin {

    public DataSource database = null;
    private Settings settings;
	private Messages m;
    private PlayersLogs pllog;
    public static Server server;
    public static Plugin authme;
    public static Permission permission;
	private static AuthMe instance;
    private Utils utils = Utils.getInstance();
    private JavaPlugin plugin;
    private FileCache playerBackup = new FileCache();
	public CitizensCommunicator citizens;
	public SendMailSSL mail = null;
	public int CitizensVersion = 0;
	public int CombatTag = 0;
	public double ChestShop = 0;
	public boolean BungeeCord = false;
	public Essentials ess;
	public Notifications notifications;
	public API api;
	public Management management;
    public HashMap<String, Integer> captcha = new HashMap<String, Integer>();
    public HashMap<String, String> cap = new HashMap<String, String>();
    public HashMap<String, String> realIp = new HashMap<String, String>();
    public List<String> premium = new ArrayList<String>();
	public MultiverseCore mv = null;
	public Location essentialsSpawn;
	public Thread databaseThread = null;

    @Override
    public void onEnable() {
    	instance = this;
    	authme = instance;

    	citizens = new CitizensCommunicator(this);

        settings = new Settings(this);
        settings.loadConfigOptions();

        setMessages(Messages.getInstance());
        pllog = PlayersLogs.getInstance();

        server = getServer();

        //Set Console Filter
        if (Settings.removePassword)
        Bukkit.getLogger().setFilter(new ConsoleFilter());

        //Load MailApi
        if(!Settings.getmailAccount.isEmpty() && !Settings.getmailPassword.isEmpty())
        	mail = new SendMailSSL(this);

		//Check Citizens Version
		citizensVersion();

		//Check Combat Tag Version
		combatTag();

		//Check Notifications
		checkNotifications();

		//Check Multiverse
		checkMultiverse();

		//Check ChestShop
		checkChestShop();
		
		//Check Essentials
		checkEssentials();

        /*
         *  Back style on start if avaible
         */
        if(Settings.isBackupActivated && Settings.isBackupOnStart) {
        Boolean Backup = new PerformBackup(this).DoBackup();
        if(Backup) ConsoleLogger.info("Backup Complete");
            else ConsoleLogger.showError("Error while making Backup");
        }

        /*
         * Backend MYSQL - FILE - SQLITE
         */
        switch (Settings.getDataSource) {
            case FILE:
            	if (Settings.useMultiThreading) {
                    FlatFileThread fileThread = new FlatFileThread();
                    fileThread.run();
                    database = fileThread;
                    databaseThread = fileThread;
                    break;
            	}
                try {
                    database = new FileDataSource();
                } catch (IOException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    if (Settings.isStopEnabled) {
                    	ConsoleLogger.showError("Can't use FLAT FILE... SHUTDOWN...");
                    	server.shutdown();
                    } 
                    if (!Settings.isStopEnabled)
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                break;
            case MYSQL:
            	if (Settings.useMultiThreading) {
                    MySQLThread sqlThread = new MySQLThread();
                    sqlThread.run();
                    database = sqlThread;
                    databaseThread = sqlThread;
                    break;
            	}
                try {
                    database = new MySQLDataSource();
                } catch (ClassNotFoundException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    if (Settings.isStopEnabled) {
                    	ConsoleLogger.showError("Can't use MySQL... Please input correct MySQL informations ! SHUTDOWN...");
                    	server.shutdown();
                    } 
                    if (!Settings.isStopEnabled)
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                } catch (SQLException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    if (Settings.isStopEnabled) {
                    	ConsoleLogger.showError("Can't use MySQL... Please input correct MySQL informations ! SHUTDOWN...");
                    	server.shutdown();
                    }
                    if (!Settings.isStopEnabled)
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                } catch(TimeoutException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    if (Settings.isStopEnabled) {
                    	ConsoleLogger.showError("Can't use MySQL... Please input correct MySQL informations ! SHUTDOWN...");
                    	server.shutdown();
                    }
                    if (!Settings.isStopEnabled)
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                break;
            case SQLITE:
            	if (Settings.useMultiThreading) {
                    SQLiteThread sqliteThread = new SQLiteThread();
                    sqliteThread.run();
                    database = sqliteThread;
                    databaseThread = sqliteThread;
                    break;
            	}
                try {
                     database = new SqliteDataSource();
                } catch (ClassNotFoundException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    if (Settings.isStopEnabled) {
                    	ConsoleLogger.showError("Can't use SQLITE... ! SHUTDOWN...");
                    	server.shutdown();
                    }
                    if (!Settings.isStopEnabled)
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                } catch (SQLException ex) {
                    ConsoleLogger.showError(ex.getMessage());
                    if (Settings.isStopEnabled) {
                    	ConsoleLogger.showError("Can't use SQLITE... ! SHUTDOWN...");
                    	server.shutdown();
                    }
                    if (!Settings.isStopEnabled)
                    this.getServer().getPluginManager().disablePlugin(this);
                    return;
                }
                break;
        }

        if (Settings.isCachingEnabled) {
            database = new CacheDataSource(this, database);
        }

        // Setup API
    	api = new API(this, database);

		// Setup Management
		management = new Management(database, this);

        PluginManager pm = getServer().getPluginManager();
        if (pm.isPluginEnabled("Spout")) {
        	pm.registerEvents(new AuthMeSpoutListener(database), this);
        	ConsoleLogger.info("Successfully hook with Spout!");
        }
        pm.registerEvents(new AuthMePlayerListener(this,database),this);
        pm.registerEvents(new AuthMeBlockListener(database, this),this);
        pm.registerEvents(new AuthMeEntityListener(database, this),this);
        if (ChestShop != 0) {
        	pm.registerEvents(new AuthMeChestShopListener(database, this), this);
        	ConsoleLogger.info("Successfully hook with ChestShop!");
        }
        if (Settings.bungee) {
        	Bukkit.getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
        	Bukkit.getMessenger().registerIncomingPluginChannel(this, "BungeeCord", new BungeeCordMessage(this));
        }

        //Find Permissions
        if (pm.getPlugin("Vault") != null) {
            RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
            if (permissionProvider != null) {
            	permission = permissionProvider.getProvider();
            	ConsoleLogger.info("Vault plugin detected, hook with " + permission.getName() + " system");
            }
            else {
            	ConsoleLogger.showError("Vault plugin is detected but not the permissions plugin!");
            }
        }

        this.getCommand("authme").setExecutor(new AdminCommand(this, database));
        this.getCommand("register").setExecutor(new RegisterCommand(database, this));
        this.getCommand("login").setExecutor(new LoginCommand(this));
        this.getCommand("changepassword").setExecutor(new ChangePasswordCommand(database, this));
        this.getCommand("logout").setExecutor(new LogoutCommand(this,database));
        this.getCommand("unregister").setExecutor(new UnregisterCommand(this, database));
        this.getCommand("passpartu").setExecutor(new PasspartuCommand(this));
        this.getCommand("email").setExecutor(new EmailCommand(this, database));
        this.getCommand("captcha").setExecutor(new CaptchaCommand(this));

        if(!Settings.isForceSingleSessionEnabled) {
            ConsoleLogger.showError("ATTENTION by disabling ForceSingleSession, your server protection is set to low");
        }

        if (Settings.reloadSupport)
        	try {
                if (!new File(getDataFolder() + File.separator + "players.yml").exists()) {
                	pllog = new PlayersLogs();
                }
                onReload();
                if (server.getOnlinePlayers().length < 1) {
                	try {
                    	PlayersLogs.players.clear();
                    	pllog.save();
                	} catch (NullPointerException npe) {
                	}
                }
        	} catch (NullPointerException ex) {
        	}
        ConsoleLogger.info("Authme " + this.getDescription().getVersion() + " enabled");
    }

	private void checkChestShop() {
    	if (!Settings.chestshop) {
    		this.ChestShop = 0;
    		return;
    	}
    	if (this.getServer().getPluginManager().isPluginEnabled("ChestShop")) {
    		try {
				String ver = com.Acrobot.ChestShop.ChestShop.getVersion();
				try {
					double version = Double.valueOf(ver.split(" ")[0]);
	    			if (version >= 3.50) {
	    				this.ChestShop = version;
	    			} else {
	    				ConsoleLogger.showError("Please Update your ChestShop version!");
	    			}
				} catch (NumberFormatException nfe) {
					try {
						double version = Double.valueOf(ver.split("t")[0]);
		    			if (version >= 3.50) {
		    				this.ChestShop = version;
		    			} else {
		    				ConsoleLogger.showError("Please Update your ChestShop version!");
		    			}
					} catch (NumberFormatException nfee) {
					}
				}
    		} catch (NullPointerException npe) {}
    		catch (NoClassDefFoundError ncdfe) {}
    		catch (ClassCastException cce) {}
    	}
	}

	private void checkMultiverse() {
		if(!Settings.multiverse) {
			mv = null;
			return;
		}
    	if (this.getServer().getPluginManager().getPlugin("Multiverse-Core") != null && this.getServer().getPluginManager().getPlugin("Multiverse-Core").isEnabled()) {
    		try {
    			mv  = (MultiverseCore) this.getServer().getPluginManager().getPlugin("Multiverse-Core");
    			ConsoleLogger.info("Hook with Multiverse-Core for SpawnLocations");
    		} catch (NullPointerException npe) {
    			mv = null;
    		} catch (ClassCastException cce) {
    			mv = null;
    		} catch (NoClassDefFoundError ncdfe) {
    			mv = null;
    		}
    	}
	}
	
	private void checkEssentials() {
    	if (this.getServer().getPluginManager().getPlugin("Essentials") != null && this.getServer().getPluginManager().getPlugin("Essentials").isEnabled()) {
    		try {
    			ess  = (Essentials) this.getServer().getPluginManager().getPlugin("Essentials");
    			ConsoleLogger.info("Hook with Essentials plugin");
    		} catch (NullPointerException npe) {
    			ess = null;
    		} catch (ClassCastException cce) {
    			ess = null;
    		} catch (NoClassDefFoundError ncdfe) {
    			ess = null;
    		}
    	}
    	if (this.getServer().getPluginManager().getPlugin("EssentialsSpawn") != null && this.getServer().getPluginManager().getPlugin("EssentialsSpawn").isEnabled()) {
    		this.essentialsSpawn = new EssSpawn().getLocation();
    		ConsoleLogger.info("Hook with EssentialsSpawn plugin");
    	}
	}

	private void checkNotifications() {
		if (!Settings.notifications) {
			this.notifications = null;
			return;
		}
		if (this.getServer().getPluginManager().getPlugin("Notifications") != null && this.getServer().getPluginManager().getPlugin("Notifications").isEnabled()) {
			this.notifications = (Notifications) this.getServer().getPluginManager().getPlugin("Notifications");
			ConsoleLogger.info("Successfully hook with Notifications");
		} else {
			this.notifications = null;
		}
	}

	private void combatTag() {
		if (this.getServer().getPluginManager().getPlugin("CombatTag") != null && this.getServer().getPluginManager().getPlugin("CombatTag").isEnabled()) {
			this.CombatTag = 1;
		} else {
			this.CombatTag = 0;
		}
	}

	private void citizensVersion() {
		if (this.getServer().getPluginManager().getPlugin("Citizens") != null && this.getServer().getPluginManager().getPlugin("Citizens").isEnabled()) {
			Citizens cit = (Citizens) this.getServer().getPluginManager().getPlugin("Citizens");
            String ver = cit.getDescription().getVersion();
            String[] args = ver.split("\\.");
            if (args[0].contains("1")) {
            	this.CitizensVersion = 1;
            } else {
            	this.CitizensVersion = 2;
            }
		} else {
			this.CitizensVersion = 0;
		}
	}

	@Override
    public void onDisable() {
        if (Bukkit.getOnlinePlayers() != null)
        for(Player player : Bukkit.getOnlinePlayers()) {
        		this.savePlayer(player);
        }
        pllog.save();

        if (database != null) {
            database.close();
        }
        
        if (databaseThread != null) {
        	databaseThread.interrupt();
        }

        if(Settings.isBackupActivated && Settings.isBackupOnStop) {
        Boolean Backup = new PerformBackup(this).DoBackup();
        if(Backup) ConsoleLogger.info("Backup Complete");
            else ConsoleLogger.showError("Error while making Backup");
        }       
        ConsoleLogger.info("Authme " + this.getDescription().getVersion() + " disabled");
    }

	private void onReload() {
		try {
	    	if (Bukkit.getServer().getOnlinePlayers() != null && !PlayersLogs.players.isEmpty()) {
	    		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
	    			if (PlayersLogs.players.contains(player.getName())) {
	    				String name = player.getName().toLowerCase();
	    		        PlayerAuth pAuth = database.getAuth(name);
	    	            if(pAuth == null)
	    	                break;
	    	            PlayerAuth auth = new PlayerAuth(name, pAuth.getHash(), pAuth.getIp(), new Date().getTime());
	    	            database.updateSession(auth);
	    				PlayerCache.getInstance().addPlayer(auth); 
	    			}
	    		}
	    	}
	    	return;
		} catch (NullPointerException ex) {
			return;
		}
    }

	public static AuthMe getInstance() {
		return instance;
	}

	public void savePlayer(Player player) throws IllegalStateException, NullPointerException {
		try {
	      if ((citizens.isNPC(player, this)) || (Utils.getInstance().isUnrestricted(player)) || (CombatTagComunicator.isNPC(player))) {
	          return;
	        }
		} catch (Exception e) { }
		try {
	        String name = player.getName().toLowerCase();
	        if ((PlayerCache.getInstance().isAuthenticated(name)) && (!player.isDead()) && 
	          (Settings.isSaveQuitLocationEnabled.booleanValue())) {
	          final PlayerAuth auth = new PlayerAuth(player.getName().toLowerCase(), (int)player.getLocation().getX(), (int)player.getLocation().getY(), (int)player.getLocation().getZ(), player.getWorld().getName());
	          Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
				@Override
				public void run() {
					database.updateQuitLoc(auth);
				}
	          });
	        }
	        if (LimboCache.getInstance().hasLimboPlayer(name))
	        {
	          LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
	          if (Settings.protectInventoryBeforeLogInEnabled.booleanValue()) {
	            player.getInventory().setArmorContents(limbo.getArmour());
	            player.getInventory().setContents(limbo.getInventory());
	          }
	          if (!limbo.getLoc().getChunk().isLoaded()) {
	        	  limbo.getLoc().getChunk().load();
	          }
	          player.teleport(limbo.getLoc());
	          this.utils.addNormal(player, limbo.getGroup());
	          player.setOp(limbo.getOperator());
	          this.plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
	          LimboCache.getInstance().deleteLimboPlayer(name);
	          if (this.playerBackup.doesCacheExist(name)) {
	            this.playerBackup.removeCache(name);
	          }
	        }
	        PlayerCache.getInstance().removePlayer(name);
	        player.saveData();
	      } catch (Exception ex) {
	      }
	}

	public CitizensCommunicator getCitizensCommunicator() {
		return citizens;
	}

	public void setMessages(Messages m) {
		this.m = m;
	}

	public Messages getMessages() {
		return m;
	}

	public Player generateKickPlayer(Player[] players) {
		Player player = null;
		int i;
		for (i = 0 ; i <= players.length ; i++) {
			Random rdm = new Random();
			int a = rdm.nextInt(players.length);
			if (!(authmePermissible(players[a], "authme.vip"))) {
				player = players[a];
				break;
			}
		}
		if (player == null) {
			for (Player p : players) {
				if (!(authmePermissible(p, "authme.vip"))) {
					player = p;
					break;
				}
			}
		}
		return player;
	}
	
	public boolean authmePermissible(Player player, String perm) {
		if (player.hasPermission(perm))
			return true;
		else if (permission != null) {
			return permission.playerHas(player, perm);
		}
		return false;
	}

	public boolean authmePermissible(CommandSender sender, String perm) {
		if (sender.hasPermission(perm)) return true;
		else if (permission != null) {
			return permission.has(sender, perm);
		}
		return false;
	}

}
