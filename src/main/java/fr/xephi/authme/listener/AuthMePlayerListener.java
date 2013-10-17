package fr.xephi.authme.listener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.api.API;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.DataFileCache;
import fr.xephi.authme.cache.backup.FileCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.events.SessionEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.PlayersLogs;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;


public class AuthMePlayerListener implements Listener {

    public static int gm = 0;
    public static HashMap<String, Integer> gameMode = new HashMap<String, Integer>();
	private Utils utils = Utils.getInstance();
    private Messages m = Messages.getInstance();
    public AuthMe plugin;
    private DataSource data;
    private FileCache playerBackup = new FileCache();

    public AuthMePlayerListener(AuthMe plugin, DataSource data) {
        this.plugin = plugin;
        this.data = data;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        if (!data.isAuthAvailable(name))
            if (!Settings.isForcedRegistrationEnabled)
                return;

        String msg = event.getMessage();
        //WorldEdit GUI Shit
        if (msg.equalsIgnoreCase("/worldedit cui"))
            return;

        String cmd = msg.split(" ")[0];
        if (cmd.equalsIgnoreCase("/login") || cmd.equalsIgnoreCase("/register") || cmd.equalsIgnoreCase("/passpartu") || cmd.equalsIgnoreCase("/l") || cmd.equalsIgnoreCase("/reg") || cmd.equalsIgnoreCase("/email") || cmd.equalsIgnoreCase("/captcha"))
            return;
        if (Settings.useEssentialsMotd && cmd.equalsIgnoreCase("/motd"))
        	return;
        if (Settings.allowCommands.contains(cmd))
        	return;

        event.setMessage("/notloggedin");
        event.setCancelled(true);
    }

    @EventHandler( priority = EventPriority.NORMAL)
    public void onPlayerNormalChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];
        
    	if (data.isAuthAvailable(name)) {
    		player.sendMessage(m._("login_msg"));
    	} else {
    		if (!Settings.isForcedRegistrationEnabled) {
    			return;
    		}
            if (Settings.emailRegistration) {
                player.sendMessage(m._("reg_email_msg"));
                return;
                } else {
                player.sendMessage(m._("reg_msg"));
                return;
                }
    	}

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler( priority = EventPriority.HIGH)
    public void onPlayerHighChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];
        
    	if (data.isAuthAvailable(name)) {
    		player.sendMessage(m._("login_msg"));
    	} else {
    		if (!Settings.isForcedRegistrationEnabled) {
    			return;
    		}
            if (Settings.emailRegistration) {
                player.sendMessage(m._("reg_email_msg"));
                return;
                } else {
                player.sendMessage(m._("reg_msg"));
                return;
                }
    	}

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler( priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];
        
    	if (data.isAuthAvailable(name)) {
    		player.sendMessage(m._("login_msg"));
    	} else {
    		if (!Settings.isForcedRegistrationEnabled) {
    			return;
    		}
            if (Settings.emailRegistration) {
                player.sendMessage(m._("reg_email_msg"));
                return;
                } else {
                player.sendMessage(m._("reg_msg"));
                return;
                }
    	}

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler( priority = EventPriority.HIGHEST)
    public void onPlayerHighestChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];
        
    	if (data.isAuthAvailable(name)) {
    		player.sendMessage(m._("login_msg"));
    	} else {
    		if (!Settings.isForcedRegistrationEnabled) {
    			return;
    		}
            if (Settings.emailRegistration) {
                player.sendMessage(m._("reg_email_msg"));
                return;
                } else {
                player.sendMessage(m._("reg_msg"));
                return;
                }
    	}

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler( priority = EventPriority.LOWEST)
    public void onPlayerEarlyChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];
        
    	if (data.isAuthAvailable(name)) {
    		player.sendMessage(m._("login_msg"));
    	} else {
    		if (!Settings.isForcedRegistrationEnabled) {
    			return;
    		}
            if (Settings.emailRegistration) {
                player.sendMessage(m._("reg_email_msg"));
                return;
                } else {
                player.sendMessage(m._("reg_msg"));
                return;
                }
    	}

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler( priority = EventPriority.LOW)
    public void onPlayerLowChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];
        
    	if (data.isAuthAvailable(name)) {
    		player.sendMessage(m._("login_msg"));
    	} else {
    		if (!Settings.isForcedRegistrationEnabled) {
    			return;
    		}
            if (Settings.emailRegistration) {
                player.sendMessage(m._("reg_email_msg"));
                } else {
                player.sendMessage(m._("reg_msg"));
                }
    	}

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        if (!Settings.isForcedRegistrationEnabled) {
            return;
        }

        if (!Settings.isMovementAllowed) {
            event.setTo(event.getFrom());
            return;
        }

        if (Settings.getMovementRadius == 0) {
            return;
        }

        int radius = Settings.getMovementRadius;
        Location spawn = plugin.getSpawnLocation(player.getWorld());

        if (!event.getPlayer().getWorld().equals(spawn.getWorld())) {
        	event.getPlayer().teleport(spawn);
        	return;
        }
        if ((spawn.distance(player.getLocation()) > radius) ) {
            event.getPlayer().teleport(spawn);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {

        final Player player = event.getPlayer();
        final String name = player.getName().toLowerCase();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (player.isOnline() && Settings.isForceSingleSessionEnabled) {
        	event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("same_nick"));
            return;
        }

        if(data.isAuthAvailable(name) && !LimboCache.getInstance().hasLimboPlayer(name)) {
        	if(!Settings.isSessionsEnabled) {
        	LimboCache.getInstance().addLimboPlayer(player , utils.removeAll(player));
        	} else if(PlayerCache.getInstance().isAuthenticated(name)) {
        		if(!Settings.sessionExpireOnIpChange)
        			if(LimboCache.getInstance().hasLimboPlayer(player.getName().toLowerCase())) {
        				LimboCache.getInstance().deleteLimboPlayer(name);  
        			}
        		LimboCache.getInstance().addLimboPlayer(player , utils.removeAll(player));
        	}
        }
        //Check if forceSingleSession is set to true, so kick player that has joined with same nick of online player
        if(player.isOnline() && Settings.isForceSingleSessionEnabled ) {
             LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName().toLowerCase()); 
             event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("same_nick"));
                    if(PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
                        utils.addNormal(player, limbo.getGroup());
                        LimboCache.getInstance().deleteLimboPlayer(player.getName().toLowerCase());
                    }            
            return;
        }

        int min = Settings.getMinNickLength;
        int max = Settings.getMaxNickLength;
        String regex = Settings.getNickRegex;

        if (name.length() > max || name.length() < min) {

            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("name_len"));
            return;
        }
        try {
            if (!player.getName().matches(regex) || name.equals("Player")) {
                try {
                	event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("regex").replaceAll("REG_EX", regex));
                } catch (StringIndexOutOfBoundsException exc) {
                	event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "allowed char : " + regex);
                }
                return;
            }
        } catch (PatternSyntaxException pse) {
        	if (regex == null || regex.isEmpty()) {
        		event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Your nickname do not match");
        		return;
        	}
            try {
            	event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("regex").replaceAll("REG_EX", regex));
            } catch (StringIndexOutOfBoundsException exc) {
            	event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "allowed char : " + regex);
            }
            return;
        }

        if (Settings.isKickNonRegisteredEnabled) {
            if (!data.isAuthAvailable(name)) {    
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("reg_only"));
                return;
            }
        }
        if (event.getResult() != PlayerLoginEvent.Result.KICK_FULL) return;
        if (player.isBanned()) return;
        if (!plugin.authmePermissible(player, "authme.vip")) {
        	event.disallow(Result.KICK_FULL, m._("kick_fullserver"));
        	return;
        }

        if (plugin.getServer().getOnlinePlayers().length > plugin.getServer().getMaxPlayers()) {
        	event.allow();
        	return;
        } else {
        	final Player pl = plugin.generateKickPlayer(plugin.getServer().getOnlinePlayers());
        	if (pl != null) {
        		pl.kickPlayer(m._("kick_forvip"));
        		event.allow();
        		return;
        	} else {
        		ConsoleLogger.info("The player " + player.getName() + " wants to join, but the server is full");
        		event.disallow(Result.KICK_FULL, m._("kick_fullserver"));
        	}
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLowestJoin(PlayerJoinEvent event) {
     if (event.getPlayer() == null) return;
     final Player player = event.getPlayer();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (Settings.bungee) {
            final ByteArrayOutputStream b = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(b);
             
            try {
                out.writeUTF("IP");
            } catch (IOException e) {
            }
            Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
                }
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        World world = player.getWorld();
        Location spawnLoc = plugin.getSpawnLocation(world);
        gm = player.getGameMode().getValue();
        final String name = player.getName().toLowerCase();
        gameMode.put(name, gm);
        BukkitScheduler sched = plugin.getServer().getScheduler();
        final PlayerJoinEvent e = event;

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (plugin.ess != null && Settings.disableSocialSpy)
        	plugin.ess.getUser(player.getName()).setSocialSpyEnabled(false);

        String ip = player.getAddress().getAddress().getHostAddress();
        if (Settings.bungee) {
        	if (plugin.realIp.containsKey(name))
        		ip = plugin.realIp.get(name);
        }
            if(Settings.isAllowRestrictedIp && !Settings.getRestrictedIp(name, ip)) {
                int gM = gameMode.get(name);
            	player.setGameMode(GameMode.getByValue(gM));
                player.kickPlayer("You are not the Owner of this account, please try another name!");
                if (Settings.banUnsafeIp)
                plugin.getServer().banIP(ip);
                return;           
            }
        if (data.isAuthAvailable(name)) {
            if (Settings.isSessionsEnabled) {
                PlayerAuth auth = data.getAuth(name);
                long timeout = Settings.getSessionTimeout * 60000;
                long lastLogin = auth.getLastLogin();
                long cur = new Date().getTime();
             if((cur - lastLogin < timeout || timeout == 0) && !auth.getIp().equals("198.18.0.1") ) {
                     if (auth.getNickname().equalsIgnoreCase(name) && auth.getIp().equals(ip) ) {
                     	plugin.getServer().getPluginManager().callEvent(new SessionEvent(auth, true));
                     	if(PlayerCache.getInstance().getAuth(name) != null) {
                     		PlayerCache.getInstance().updatePlayer(auth);
                     	} else {
                     		PlayerCache.getInstance().addPlayer(auth);
                     	}
                         player.sendMessage(m._("valid_session"));
                         return;
                     } else if (!Settings.sessionExpireOnIpChange){
                     	int gM = gameMode.get(name);
                     	player.setGameMode(GameMode.getByValue(gM));
                     	player.kickPlayer(m._("unvalid_session"));
                     	return;
                     } else if (auth.getNickname().equalsIgnoreCase(name)){
                         if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin)
                         	sched.scheduleSyncDelayedTask(plugin, new Runnable() {
                         		public void run() {
                         			e.getPlayer().setGameMode(GameMode.SURVIVAL);
                         		}
                         	});
                		 //Player change his IP between 2 relog-in
                         PlayerCache.getInstance().removePlayer(name);
                         LimboCache.getInstance().addLimboPlayer(player , utils.removeAll(player));
                	 } else {
                      	int gM = gameMode.get(name);
                     	player.setGameMode(GameMode.getByValue(gM));
                     	player.kickPlayer(m._("unvalid_session"));
                     	return;
                	 }
            } else {
            	//Session is ended correctly
                PlayerCache.getInstance().removePlayer(name);
                LimboCache.getInstance().addLimboPlayer(player , utils.removeAll(player));
                }
          }
          // isent in session or session was ended correctly
            if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin)
             			e.getPlayer().setGameMode(GameMode.SURVIVAL);
            if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled  && Settings.getForcedWorlds.contains(player.getWorld().getName()))) {
                SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, PlayerCache.getInstance().isAuthenticated(name));
                plugin.getServer().getPluginManager().callEvent(tpEvent);
                if(!tpEvent.isCancelled()) {
                	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                	}
              	  player.teleport(tpEvent.getTo());
                }
            }
            placePlayerSafely(player, spawnLoc);
            LimboCache.getInstance().updateLimboPlayer(player);
            DataFileCache dataFile = new DataFileCache(LimboCache.getInstance().getLimboPlayer(name).getInventory(),LimboCache.getInstance().getLimboPlayer(name).getArmour());
            playerBackup.createCache(name, dataFile, LimboCache.getInstance().getLimboPlayer(name).getGroup(),LimboCache.getInstance().getLimboPlayer(name).getOperator(),LimboCache.getInstance().getLimboPlayer(name).isFlying());
        } else {
            if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin)
             			e.getPlayer().setGameMode(GameMode.SURVIVAL);
            if(!Settings.unRegisteredGroup.isEmpty()){
               utils.setGroup(player, Utils.groupType.UNREGISTERED);
            }
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        if(Settings.protectInventoryBeforeLogInEnabled) {
        	try {
        		LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName().toLowerCase());
            	ProtectInventoryEvent ev = new ProtectInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
            	plugin.getServer().getPluginManager().callEvent(ev);
            	if (ev.isCancelled()) {
            		if (!Settings.noConsoleSpam)
            			ConsoleLogger.info("ProtectInventoryEvent has been cancelled for " + player.getName() + " ...");
            	} else {
            		API.setPlayerInventory(player, ev.getEmptyInventory(), ev.getEmptyArmor());
            	}
        	} catch (NullPointerException ex) {
        	}
        }
        String msg = "";
        if(Settings.emailRegistration) {
        	msg = data.isAuthAvailable(name) ? m._("login_msg") : m._("reg_email_msg");
        } else {
        	msg = data.isAuthAvailable(name) ? m._("login_msg") : m._("reg_msg");
        }
        int time = Settings.getRegistrationTimeout * 20;
        int msgInterval = Settings.getWarnMessageInterval;
        if (time != 0) {
            BukkitTask id = sched.runTaskLater(plugin, new TimeoutTask(plugin, name), time);
            if(!LimboCache.getInstance().hasLimboPlayer(name))
                 LimboCache.getInstance().addLimboPlayer(player);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id.getTaskId());
        }
        if(!LimboCache.getInstance().hasLimboPlayer(name))
            LimboCache.getInstance().addLimboPlayer(player);
        if(player.isOp())
            player.setOp(false);
        if (!Settings.isMovementAllowed) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        BukkitTask msgT = sched.runTask(plugin, new MessageTask(plugin, name, msg, msgInterval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT.getTaskId());
        player.setNoDamageTicks(Settings.getRegistrationTimeout * 20);
        if (Settings.useEssentialsMotd)
        	player.performCommand("motd");
    }

	private void placePlayerSafely(Player player, Location spawnLoc) {
		if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled  && Settings.getForcedWorlds.contains(player.getWorld().getName())))
			return;
		Block b = player.getLocation().getBlock();
		if (b.getType() == Material.PORTAL || b.getType() == Material.ENDER_PORTAL || b.getType() == Material.LAVA || b.getType() == Material.STATIONARY_LAVA) {
			player.sendMessage(m._("unsafe_spawn"));
			player.teleport(spawnLoc);
			return;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();
        Location loc = player.getLocation();
        if (loc.getY() % 1 != 0)
        	loc.add(0, 0.5, 0);

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (PlayerCache.getInstance().isAuthenticated(name) && !player.isDead()) {
        	if(Settings.isSaveQuitLocationEnabled && data.isAuthAvailable(name)) {
        		final PlayerAuth auth = new PlayerAuth(name,loc.getBlockX(),loc.getBlockY(),loc.getBlockZ(),loc.getWorld().getName());
        		try {
        	        if (data instanceof Thread) {
        	        	data.updateQuitLoc(auth);
        	        } else {
        	            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
        	    			@Override
        	    			public void run() {
        	    				data.updateQuitLoc(auth);
        	    			}
        	            });
        	        }
        		} catch (NullPointerException npe) { }
        	}
        } 

        if (LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if(Settings.protectInventoryBeforeLogInEnabled && player.hasPlayedBefore()) {
            	RestoreInventoryEvent ev = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
            	plugin.getServer().getPluginManager().callEvent(ev);
            	if (!ev.isCancelled()) {
            		API.setPlayerInventory(player, limbo.getInventory(), limbo.getArmour());
            	}
            }
            utils.addNormal(player, limbo.getGroup());
            player.setOp(limbo.getOperator());
            if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
                player.setAllowFlight(limbo.isFlying());
                player.setFlying(limbo.isFlying());
            }
            this.plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
            LimboCache.getInstance().deleteLimboPlayer(name);
            if(playerBackup.doesCacheExist(name)) {
                        playerBackup.removeCache(name);
            }
        }
        try {
        	PlayerCache.getInstance().removePlayer(name);
        	PlayersLogs.players.remove(player.getName());
        	PlayersLogs.getInstance().save();
        	player.getVehicle().eject();
        } catch (NullPointerException ex) {
        }
        if (gameMode.containsKey(name)) gameMode.remove(name);
        player.saveData();
    }

	@EventHandler(priority=EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
      if (event.getPlayer() == null) {
        return;
      }
      if (event.isCancelled()) {
    	  return;
      }

      Player player = event.getPlayer();
      Location loc = player.getLocation();
      if (loc.getY() % 1 != 0)
      	loc.add(0, 0.5, 0);

      if ((plugin.getCitizensCommunicator().isNPC(player, plugin)) || (Utils.getInstance().isUnrestricted(player)) || (CombatTagComunicator.isNPC(player))) {
        return;
      }

      if ((Settings.isForceSingleSessionEnabled) && 
        (event.getReason().contains("You logged in from another location"))) {
        event.setCancelled(true);
        return;
      }

      String name = player.getName().toLowerCase();
      if ((PlayerCache.getInstance().isAuthenticated(name)) && (!player.isDead()) && 
        (Settings.isSaveQuitLocationEnabled)  && data.isAuthAvailable(name)) {
        final PlayerAuth auth = new PlayerAuth(name, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),loc.getWorld().getName());
		try {
	        if (data instanceof Thread) {
	        	data.updateQuitLoc(auth);
	        } else {
	            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){
	    			@Override
	    			public void run() {
	    				data.updateQuitLoc(auth);
	    			}
	            });
	        }
		} catch (NullPointerException npe) { }
      }

      if (LimboCache.getInstance().hasLimboPlayer(name))
      {
        LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
        if (Settings.protectInventoryBeforeLogInEnabled) {
        	try {
            	RestoreInventoryEvent ev = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
            	plugin.getServer().getPluginManager().callEvent(ev);
            	if (!ev.isCancelled()) {
            		API.setPlayerInventory(player, ev.getInventory(), ev.getArmor());
            	}
        	} catch (NullPointerException npe){
        		ConsoleLogger.showError("Problem while restore " + name + "inventory after a kick");
        	}
        }
        try {
            AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
            plugin.getServer().getPluginManager().callEvent(tpEvent);
            if(!tpEvent.isCancelled()) {
            	if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
            		tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
            	}
          	  player.teleport(tpEvent.getTo());
            }
        } catch (NullPointerException npe) {
        }
        this.utils.addNormal(player, limbo.getGroup());
        player.setOp(limbo.getOperator());
        if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
            player.setAllowFlight(limbo.isFlying());
            player.setFlying(limbo.isFlying());
        }
        this.plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
        LimboCache.getInstance().deleteLimboPlayer(name);
        if (this.playerBackup.doesCacheExist(name)) {
          this.playerBackup.removeCache(name);
        }
      }
      try {
      	PlayerCache.getInstance().removePlayer(name);
      	PlayersLogs.players.remove(player.getName());
      	PlayersLogs.getInstance().save();
      	if (gameMode.containsKey(name)) gameMode.remove(name);
      	player.getVehicle().eject();
      	player.saveData();
      } catch (NullPointerException ex) {}
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if(plugin.getCitizensCommunicator().isNPC(player, plugin))
        	return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
        	if (!Settings.isForcedRegistrationEnabled) {
        		return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) return;

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if(plugin.getCitizensCommunicator().isNPC(player, plugin))
        	return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() != Material.AIR)
        	event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInventoryOpen(InventoryOpenEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) return;
        if (!(event.getPlayer() instanceof Player)) return;
        Player player = (Player) event.getPlayer();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if(plugin.getCitizensCommunicator().isNPC(player, plugin))
        	return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled() || event.getWhoClicked() == null) return;
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if(plugin.getCitizensCommunicator().isNPC(player, plugin))
        	return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setResult(org.bukkit.event.Event.Result.DENY);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if(plugin.getCitizensCommunicator().isNPC(player, plugin))
        	return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if (PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        if (event.isCancelled() || event.getPlayer() == null || event == null) {
            return;
        }
        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();
        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }
        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer() == null || event == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        if (Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player))
            return;

        if(plugin.getCitizensCommunicator().isNPC(player, plugin))
        	return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        if (!data.isAuthAvailable(name))
            if (!Settings.isForcedRegistrationEnabled)
                return;

        if (!Settings.isTeleportToSpawnEnabled && !Settings.isForceSpawnLocOnJoinEnabled)
        	return;
        
        Location spawn = plugin.getSpawnLocation(player.getWorld());
        event.setRespawnLocation(spawn);
    }

}
