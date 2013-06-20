package uk.org.whoami.authme.threads;

import java.security.NoSuchAlgorithmException;
import java.util.Date;

import me.muizers.Notifications.Notification;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.ConsoleLogger;
import uk.org.whoami.authme.Utils;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.cache.limbo.LimboCache;
import uk.org.whoami.authme.cache.limbo.LimboPlayer;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.events.RegisterTeleportEvent;
import uk.org.whoami.authme.security.PasswordSecurity;
import uk.org.whoami.authme.security.RandomString;
import uk.org.whoami.authme.settings.Messages;
import uk.org.whoami.authme.settings.PlayersLogs;
import uk.org.whoami.authme.settings.Settings;
import uk.org.whoami.authme.settings.Spawn;
import uk.org.whoami.authme.task.MessageTask;
import uk.org.whoami.authme.task.TimeoutTask;

public class RegisterThread extends Thread {

    private Messages m = Messages.getInstance();
    private PlayersLogs pllog = PlayersLogs.getInstance();
    private DataSource database;
    private boolean isFirstTimeJoin;
	private PlayerAuth auth;
	private AuthMe plugin;
	private Player player;
	private String[] args;
	private String ip;

	public RegisterThread(AuthMe plugin, DataSource database, Player player, String ip, String[] args) {
        this.database = database;
        this.setFirstTimeJoin(false);
        this.plugin = plugin;
        this.player = player;
        this.args = args.clone();
        this.ip = ip;
	}
	public void run() {
		final String name = player.getName().toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            player.sendMessage(m._("logged_in"));
            this.interrupt();
            return;
        }

        if (!Settings.isRegistrationEnabled) {
            player.sendMessage(m._("reg_disabled"));
            this.interrupt();
            return;
        }

        if (database.isAuthAvailable(player.getName().toLowerCase())) {
            player.sendMessage(m._("user_regged"));
            if (pllog.getStringList("players").contains(player.getName())) {
           	 pllog.getStringList("players").remove(player.getName());
            }
            this.interrupt();
            return;
        }

        if(Settings.getmaxRegPerIp > 0 ){
        	if(!plugin.authmePermissible(player, "authme.allow2accounts") && database.getAllAuthsByIp(ip).size() >= Settings.getmaxRegPerIp) {
        		player.sendMessage(m._("max_reg"));
        		this.interrupt();
                return;
        	}
        }

        if(Settings.emailRegistration && !Settings.getmailAccount.isEmpty()) {
        	if(!args[0].contains("@")) {
                player.sendMessage(m._("usage_reg"));
                this.interrupt();
                return;
        	}
        	if(Settings.doubleEmailCheck) {
        		if(args.length < 2) {
                    player.sendMessage(m._("usage_reg"));
                    this.interrupt();
                    return;
        		}
        		if(!args[0].equals(args[1])) {
                    player.sendMessage(m._("usage_reg"));
                    this.interrupt();
                    return;
        		}
        	}
        	final String email = args[0];
        	if(Settings.getmaxRegPerEmail > 0) {
        		if (!plugin.authmePermissible(player, "authme.allow2accounts") && database.getAllAuthsByEmail(email).size() >= Settings.getmaxRegPerEmail) {
        			player.sendMessage(m._("max_reg"));
        			this.interrupt();
        			return;
        		}
        	}
			RandomString rand = new RandomString(Settings.getRecoveryPassLength);
			final String thePass = rand.nextString();
            if (!thePass.isEmpty()) {
            	if (PasswordSecurity.userSalt.containsKey(name)) {
        			try {
        				final String hashnew = PasswordSecurity.getHash(Settings.getPasswordHash, thePass, name);
	            		final PlayerAuth fAuth = new PlayerAuth(name, hashnew, PasswordSecurity.userSalt.get(name), ip, new Date().getTime(), (int) player.getLocation().getX() , (int) player.getLocation().getY(), (int) player.getLocation().getZ(), player.getWorld().getName(), email);
        	            database.saveAuth(fAuth);
						database.updateEmail(fAuth);
						database.updateSession(fAuth);
						plugin.mail.main(fAuth, thePass);
        			} catch (NoSuchAlgorithmException e) {
        				ConsoleLogger.showError(e.getMessage());
        			}
            	} else {
        			try {
        				final String hashnew = PasswordSecurity.getHash(Settings.getPasswordHash, thePass, name);
        				final PlayerAuth fAuth = new PlayerAuth(name, hashnew, ip, new Date().getTime(), (int) player.getLocation().getX() , (int) player.getLocation().getY(), (int) player.getLocation().getZ(), player.getWorld().getName(), email);
        				database.saveAuth(fAuth);
						database.updateEmail(fAuth);
						database.updateSession(fAuth);
						plugin.mail.main(fAuth, thePass);
        			} catch (NoSuchAlgorithmException e) {
        				ConsoleLogger.showError(e.getMessage());
        			}
            	}
                if(!Settings.getRegisteredGroup.isEmpty()){
                    Utils.getInstance().setGroup(player, Utils.groupType.REGISTERED);
                }
            	player.sendMessage(m._("vb_nonActiv"));
            	String msg = m._("login_msg");
            	int time = Settings.getRegistrationTimeout * 20;
            	int msgInterval = Settings.getWarnMessageInterval;
                if (time != 0) {
                	Bukkit.getScheduler().cancelTask(LimboCache.getInstance().getLimboPlayer(name).getTimeoutTaskId());
                    BukkitTask id = Bukkit.getScheduler().runTaskLater(plugin, new TimeoutTask(plugin, name), time);
                    LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id.getTaskId());
                }

                Bukkit.getScheduler().cancelTask(LimboCache.getInstance().getLimboPlayer(name).getMessageTaskId());
                BukkitTask nwMsg = Bukkit.getScheduler().runTask(plugin, new MessageTask(plugin, name, msg, msgInterval));
                LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(nwMsg.getTaskId());

            	LimboCache.getInstance().deleteLimboPlayer(name);
                if (Settings.isTeleportToSpawnEnabled) {
                	World world = player.getWorld();
                	Location loca = world.getSpawnLocation();
                	if (plugin.mv != null) {
                		try {
                			loca = plugin.mv.getMVWorldManager().getMVWorld(world).getSpawnLocation();
                		} catch (NullPointerException npe) {
                		} catch (ClassCastException cce) {
                		} catch (NoClassDefFoundError ncdfe) {
                		}
                	}
                    if (plugin.essentialsSpawn != null) {
                    	loca = plugin.essentialsSpawn;
                    }
                	if (Spawn.getInstance().getLocation() != null)
                		loca = Spawn.getInstance().getLocation();
                	final Location locaT = loca;
                	Bukkit.getScheduler().runTask(plugin, new Runnable() {
						@Override
						public void run() {
		                    RegisterTeleportEvent tpEvent = new RegisterTeleportEvent(player, locaT);
		                    plugin.getServer().getPluginManager().callEvent(tpEvent);
		                    if(!tpEvent.isCancelled()) {
		                    	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
		                    		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
		                    	}
		                  	  	player.teleport(tpEvent.getTo());
		                    }
						}
                	});
                }
                this.setFirstTimeJoin(true);
                player.saveData();
                if (!Settings.noConsoleSpam)
                ConsoleLogger.info(player.getName() + " registered "+player.getAddress().getAddress().getHostAddress());
                if(plugin.notifications != null) {
                	plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " has registered!"));
                }
                this.interrupt();
            	return;
            }
        }

        if (args.length == 0 || (Settings.getEnablePasswordVerifier && args.length < 2) ) {
            player.sendMessage(m._("usage_reg"));
            this.interrupt();
            return;
        }

        if(args[0].length() < Settings.getPasswordMinLen || args[0].length() > Settings.passwordMaxLength) {
            player.sendMessage(m._("pass_len"));
            this.interrupt();
            return;
        }
        try {
            String hash;
            if(Settings.getEnablePasswordVerifier) {
                if (args[0].equals(args[1])) {
                    hash = PasswordSecurity.getHash(Settings.getPasswordHash, args[0], name);
                 } else {
                    player.sendMessage(m._("password_error"));
                    this.interrupt();
                    return;
                  }
            } else
                hash = PasswordSecurity.getHash(Settings.getPasswordHash, args[0], name);
            if (Settings.getMySQLColumnSalt.isEmpty())
            {
            	auth = new PlayerAuth(name, hash, ip, new Date().getTime());
            } else {
            	auth = new PlayerAuth(name, hash, PasswordSecurity.userSalt.get(name), ip, new Date().getTime());
            }
            if (!database.saveAuth(auth)) {
                player.sendMessage(m._("error"));
                this.interrupt();
                return;
            }
            PlayerCache.getInstance().addPlayer(auth);
            final LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (limbo != null) {
            	Bukkit.getScheduler().runTask(plugin, new Runnable(){
					@Override
					public void run() {
						player.setGameMode(GameMode.getByValue(limbo.getGameMode()));
					}
            	});
                if (Settings.isTeleportToSpawnEnabled) {
                	World world = player.getWorld();
                	Location loca = world.getSpawnLocation();
                	if (plugin.mv != null) {
                		try {
                			loca = plugin.mv.getMVWorldManager().getMVWorld(world).getSpawnLocation();
                		} catch (NullPointerException npe) {
                			
                		} catch (ClassCastException cce) {
                			
                		} catch (NoClassDefFoundError ncdfe) {
                			
                		}
                	}
                    if (plugin.essentialsSpawn != null) {
                    	loca = plugin.essentialsSpawn;
                    }
                	if (Spawn.getInstance().getLocation() != null)
                		loca = Spawn.getInstance().getLocation();
                	final Location locaT = loca;
                	Bukkit.getScheduler().runTask(plugin, new Runnable(){
    					@Override
    					public void run() {
    	                    RegisterTeleportEvent tpEvent = new RegisterTeleportEvent(player, locaT);
    	                    plugin.getServer().getPluginManager().callEvent(tpEvent);
    	                    if(!tpEvent.isCancelled()) {
    	                    	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
    	                    		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
    	                    	}
    	                  	  	player.teleport(tpEvent.getTo());
    	                    }
    					}
                	});
                }
                player.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
                player.getServer().getScheduler().cancelTask(limbo.getMessageTaskId());
                LimboCache.getInstance().deleteLimboPlayer(name);
            }

            if(!Settings.getRegisteredGroup.isEmpty()){
                Utils.getInstance().setGroup(player, Utils.groupType.REGISTERED);
            }
            player.sendMessage(m._("registered"));
            if (!Settings.getmailAccount.isEmpty())
            player.sendMessage(m._("add_email"));
            this.setFirstTimeJoin(true);
            player.saveData();
            if (!Settings.noConsoleSpam)
            ConsoleLogger.info(player.getName() + " registered "+player.getAddress().getAddress().getHostAddress());
            if(plugin.notifications != null) {
            	plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " has registered!"));
            }
            this.interrupt();
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            player.sendMessage(m._("error"));
            this.interrupt();
        }
	}
	public void setFirstTimeJoin(boolean isFirstTimeJoin) {
		this.isFirstTimeJoin = isFirstTimeJoin;
	}
	public boolean isFirstTimeJoin() {
		return isFirstTimeJoin;
	}
}
