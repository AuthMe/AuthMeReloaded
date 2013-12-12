package fr.xephi.authme;

import java.util.Date;
import java.util.List;

import me.muizers.Notifications.Notification;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;

import fr.xephi.authme.api.API;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.FileCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.LoginEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.PlayersLogs;
import fr.xephi.authme.settings.Settings;

/**
 * 
 * @authors Xephi59, <a href="http://dev.bukkit.org/profiles/Possible/">Possible</a>
 *
 */
public class Management {
    private Messages m = Messages.getInstance();
    private PlayersLogs pllog = PlayersLogs.getInstance();
    private Utils utils = Utils.getInstance();
    private FileCache playerCache = new FileCache();
    private DataSource database;
    public AuthMe plugin;
    public static RandomString rdm = new RandomString(Settings.captchaLength);
    public PluginManager pm;

    public Management(DataSource database, AuthMe plugin) {
        this.database = database;
        this.plugin = plugin;
        this.pm = plugin.getServer().getPluginManager();
    }

    public void performLogin(final Player player, final String password, final boolean passpartu, final boolean forceLogin) {
        if (passpartu) {
            // Passpartu-Login Bypasses Password-Authentication.
            new AsyncronousPasspartuLogin(player).pass();
        } else {
        	new AsyncronousLogin(player, password, forceLogin).process();
        }
    }

    class AsyncronousLogin {
        protected Player player;
        protected String name;
        protected String password;
        protected String realName;
        protected boolean forceLogin;

        public AsyncronousLogin(Player player, String password, boolean forceLogin) {
            this.player = player;
            this.password = password;
            name = player.getName().toLowerCase();
            realName = player.getName();
            this.forceLogin = forceLogin;
        }

        protected String getIP() {
            String ip = player.getAddress().getAddress().getHostAddress();
            if (Settings.bungee) {
                if (plugin.realIp.containsKey(name))
                    ip = plugin.realIp.get(name);
            }
            return ip;
        }
        protected boolean needsCaptcha() {
            if (Settings.useCaptcha) {
                if (!plugin.captcha.containsKey(name)) {
                    plugin.captcha.put(name, 1);
                } else {
                    int i = plugin.captcha.get(name) + 1;
                    plugin.captcha.remove(name);
                    plugin.captcha.put(name, i);
                }
                if (plugin.captcha.containsKey(name) && plugin.captcha.get(name) >= Settings.maxLoginTry) {
                    plugin.cap.put(name, rdm.nextString());
                    player.sendMessage(m._("need_captcha").replace("THE_CAPTCHA", plugin.cap.get(name)).replace("<theCaptcha>", plugin.cap.get(name)));
                    return true;
                } else if (plugin.captcha.containsKey(name) && plugin.captcha.get(name) >= Settings.maxLoginTry) {
                    try {
                        plugin.captcha.remove(name);
                        plugin.cap.remove(name);
                    } catch (NullPointerException npe) {
                    }
                }
            }
            return false;
        }

        /**
         * Checks the precondition for authentication (like user known) and returns the playerAuth-State
         */
        protected PlayerAuth preAuth() {
            if (PlayerCache.getInstance().isAuthenticated(name)) {
                player.sendMessage(m._("logged_in"));
                return null;
            }
            if (!database.isAuthAvailable(name)) {
                player.sendMessage(m._("user_unknown"));
                return null;
            }
            PlayerAuth pAuth = database.getAuth(name);
            if (pAuth == null) {
                player.sendMessage(m._("user_unknown"));
                return null;
            }
            if (!Settings.getMySQLColumnGroup.isEmpty() && pAuth.getGroupId() == Settings.getNonActivatedGroup) {
                player.sendMessage(m._("vb_nonActiv"));
                return null;
            }
            return pAuth;
        }

        protected void process() {
            PlayerAuth pAuth = preAuth();
            if (pAuth == null || needsCaptcha())
                return;

            String hash = pAuth.getHash();
            String email = pAuth.getEmail();
            boolean passwordVerified = true;
            if (!forceLogin)
            	try {
            		passwordVerified = PasswordSecurity.comparePasswordWithHash(password, hash, name);
            	} catch (Exception ex) {
            		ConsoleLogger.showError(ex.getMessage());
            		player.sendMessage(m._("error"));
            		return;
            	}
            if (passwordVerified && player.isOnline()) {
                PlayerAuth auth = new PlayerAuth(name, hash, getIP(), new Date().getTime(), email, realName);
                database.updateSession(auth);

                /*
                 * Little Work Around under Registration Group Switching for
                 * admins that add Registration thru a web Scripts.
                 */
                if (Settings.isPermissionCheckEnabled
                        && AuthMe.permission.playerInGroup(player, Settings.unRegisteredGroup)
                        && !Settings.unRegisteredGroup.isEmpty()) {
                    AuthMe.permission
                            .playerRemoveGroup(player.getWorld(), player.getName(), Settings.unRegisteredGroup);
                    AuthMe.permission.playerAddGroup(player.getWorld(), player.getName(), Settings.getRegisteredGroup);
                }

                pllog.addPlayer(player);

                if (Settings.useCaptcha) {
                    if (plugin.captcha.containsKey(name)) {
                        plugin.captcha.remove(name);
                    }
                    if (plugin.cap.containsKey(name)) {
                        plugin.cap.remove(name);
                    }
                }

                player.setNoDamageTicks(0);
                player.sendMessage(m._("login"));

                displayOtherAccounts(auth);

                if (!Settings.noConsoleSpam)
                    ConsoleLogger.info(player.getName() + " logged in!");

                if (plugin.notifications != null) {
                    plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " logged in!"));
                }

                // makes player isLoggedin via API
                PlayerCache.getInstance().addPlayer(auth);

                // As the scheduling executes the Task most likely after the current task, we schedule it in the end
                // so that we can be sure, and have not to care if it might be processed in other order.
                ProcessSyncronousPlayerLogin syncronousPlayerLogin = new ProcessSyncronousPlayerLogin(player);
                if (syncronousPlayerLogin.getLimbo() != null) {
                    player.getServer().getScheduler().cancelTask(syncronousPlayerLogin.getLimbo().getTimeoutTaskId());
                    player.getServer().getScheduler().cancelTask(syncronousPlayerLogin.getLimbo().getMessageTaskId());
                }
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, syncronousPlayerLogin);
            } else if (player.isOnline()) {
                if (!Settings.noConsoleSpam)
                    ConsoleLogger.info(player.getName() + " used the wrong password");
                if (Settings.isKickOnWrongPasswordEnabled) {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (AuthMePlayerListener.gameMode != null && AuthMePlayerListener.gameMode.containsKey(name)) {
                                player.setGameMode(GameMode.getByValue(AuthMePlayerListener.gameMode.get(name)));
                            }
                            player.kickPlayer(m._("wrong_pwd"));
                        }
                    });
                } else {
                    player.sendMessage(m._("wrong_pwd"));
                    return;
                }
            } else {
                ConsoleLogger.showError("Player " + name + " wasn't online during login process, aborted... ");
            }
        }
    }

    class AsyncronousPasspartuLogin extends AsyncronousLogin {
        public AsyncronousPasspartuLogin(Player player) {
            super(player, null, false);
        }

        public void pass() {
            PlayerAuth pAuth = preAuth();
            if (pAuth == null)
                return;

            String hash = pAuth.getHash();
            String email = pAuth.getEmail();

            PlayerAuth auth = new PlayerAuth(name, hash, getIP(), new Date().getTime(), email, realName);
            database.updateSession(auth);

            /*
             * Little Work Around under Registration Group Switching for
             * admins that add Registration thru a web Scripts.
             */
            if (Settings.isPermissionCheckEnabled
                    && AuthMe.permission.playerInGroup(player, Settings.unRegisteredGroup)
                    && !Settings.unRegisteredGroup.isEmpty()) {
                AuthMe.permission
                        .playerRemoveGroup(player.getWorld(), player.getName(), Settings.unRegisteredGroup);
                AuthMe.permission.playerAddGroup(player.getWorld(), player.getName(), Settings.getRegisteredGroup);
            }

            pllog.addPlayer(player);

            if (Settings.useCaptcha) {
                if (plugin.captcha.containsKey(name)) {
                    plugin.captcha.remove(name);
                }
                if (plugin.cap.containsKey(name)) {
                    plugin.cap.remove(name);
                }
            }

            player.setNoDamageTicks(0);
            player.sendMessage(m._("login"));

            displayOtherAccounts(auth);

            if (!Settings.noConsoleSpam)
                ConsoleLogger.info(player.getName() + " logged in!");

            if (plugin.notifications != null) {
                plugin.notifications.showNotification(new Notification("[AuthMe] " + player.getName() + " logged in!"));
            }

            // makes player isLoggedin via API
            PlayerCache.getInstance().addPlayer(auth);

            // As the scheduling executes the Task most likely after the current task, we schedule it in the end
            // so that we can be sure, and have not to care if it might be processed in other order.
            ProcessSyncronousPlayerLogin syncronousPlayerLogin = new ProcessSyncronousPlayerLogin(player);
            if (syncronousPlayerLogin.getLimbo() != null) {
                player.getServer().getScheduler().cancelTask(syncronousPlayerLogin.getLimbo().getTimeoutTaskId());
                player.getServer().getScheduler().cancelTask(syncronousPlayerLogin.getLimbo().getMessageTaskId());
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, syncronousPlayerLogin);
        }
    }

    class ProcessSyncronousPlayerLogin implements Runnable {
        private LimboPlayer limbo;
        private Player player;
        private String name;
        private PlayerAuth auth;
        public ProcessSyncronousPlayerLogin(Player player) {
            this.player = player;
            this.name = player.getName().toLowerCase();
            this.limbo = LimboCache.getInstance().getLimboPlayer(name);
            this.auth = database.getAuth(name);
        }

        public LimboPlayer getLimbo() {
            return limbo;
        }

        protected void restoreOpState() {
            player.setOp(limbo.getOperator());
            if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
                player.setAllowFlight(limbo.isFlying());
                player.setFlying(limbo.isFlying());
            }
        }
        protected void packQuitLocation() {
            utils.packCoords(auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ(), auth.getWorld(), player);
        }
        protected void teleportBackFromSpawn() {
            AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
            pm.callEvent(tpEvent);
            if (!tpEvent.isCancelled()) {
                Location fLoc = tpEvent.getTo();
                if (!fLoc.getChunk().isLoaded()) {
                    fLoc.getChunk().load();
                }
                player.teleport(fLoc);
            }
        }
        protected void teleportToSpawn() {
            Location spawnL = plugin.getSpawnLocation(player.getWorld());
            SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnL, true);
            pm.callEvent(tpEvent);
            if (!tpEvent.isCancelled()) {
                Location fLoc = tpEvent.getTo();
                if (!fLoc.getChunk().isLoaded()) {
                    fLoc.getChunk().load();
                }
                player.teleport(fLoc);
            }
        }
        protected void restoreInventory() {
            RestoreInventoryEvent event = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
            Bukkit.getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                API.setPlayerInventory(player, event.getInventory(), event.getArmor());
            }
        }
        protected void forceCommands() {
        	for (String command : Settings.forceCommands) {
        		try {
        			player.performCommand(command.replace("%p", player.getName()));
        		} catch (Exception e) {}
        	}
        }

        @Override
        public void run() {
             // Limbo contains the State of the Player before /login
            if (limbo != null) {
                // Op & Flying
                restoreOpState();

                /*
                 * Restore Inventories and GameMode
                 * We need to restore them before teleport the player
                 * Cause in AuthMePlayerListener, we call ProtectInventoryEvent after Teleporting
                 * Also it's the current world inventory !
                 */
                if (!Settings.forceOnlyAfterLogin) {
                	player.setGameMode(GameMode.getByValue(limbo.getGameMode()));
                    // Inventory - Make it after restore GameMode , cause we need to restore the
                    // right inventory in the right gamemode
                    if (Settings.protectInventoryBeforeLogInEnabled && player.hasPlayedBefore()) {
                        restoreInventory();
                    }
                }
                else {
                    // Inventory - Make it before force the survival GameMode to cancel all
                	// inventory problem
                    if (Settings.protectInventoryBeforeLogInEnabled && player.hasPlayedBefore()) {
                        restoreInventory();
                    }
                    player.setGameMode(GameMode.SURVIVAL);
                }

                // Teleport
                if (Settings.isTeleportToSpawnEnabled && !Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())) {
                    if (Settings.isSaveQuitLocationEnabled && auth.getQuitLocY() != 0) {
                        packQuitLocation();
                    } else {
                        teleportBackFromSpawn();
                    }
                } else if (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())) {
                    teleportToSpawn();
                } else if (Settings.isSaveQuitLocationEnabled && auth.getQuitLocY() != 0) {
                    packQuitLocation();
                } else {
                    teleportBackFromSpawn();
                }
                
                // Re-Force Survival GameMode if we need due to world change specification
                if (Settings.isForceSurvivalModeEnabled)
                	Utils.forceGM(player);
                
                // Cleanup no longer used temporary data
                LimboCache.getInstance().deleteLimboPlayer(name);
                if (playerCache.doesCacheExist(name)) {
                    playerCache.removeCache(name);
                }
            }
            
            // We can now display the join message
            if (AuthMePlayerListener.joinMessage.containsKey(name) && AuthMePlayerListener.joinMessage.get(name) != null) {
            	for (Player p : Bukkit.getServer().getOnlinePlayers()) {
            		if (p.isOnline())
            			p.sendMessage(AuthMePlayerListener.joinMessage.get(name));
            	}
            }
            
            // The Loginevent now fires (as intended) after everything is processed
            Bukkit.getServer().getPluginManager().callEvent(new LoginEvent(player, true));
            player.saveData();
            
            // Login is now finish , we can force all commands
            forceCommands();
        }
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
        if (this.database.getAllAuthsByName(auth).size() == 1) {
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
            if (plugin.authmePermissible(player, "authme.seeOtherAccounts")) {
                player.sendMessage("[AuthMe] The player " + auth.getNickname() + " has "
                        + String.valueOf(accountList.size()) + " accounts");
                player.sendMessage(message);
            }
        }
    }
}
