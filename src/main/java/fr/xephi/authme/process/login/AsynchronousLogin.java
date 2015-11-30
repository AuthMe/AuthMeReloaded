package fr.xephi.authme.process.login;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeAsyncPreLoginEvent;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.permission.UserPermission;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Date;
import java.util.List;

/**
 */
public class AsynchronousLogin {

    private static final RandomString rdm = new RandomString(Settings.captchaLength);
    protected final Player player;
    protected final String name;
    protected final String realName;
    protected final String password;
    protected final boolean forceLogin;
    private final AuthMe plugin;
    private final DataSource database;
    private final Messages m;

    /**
     * Constructor for AsynchronousLogin.
     *
     * @param player     Player
     * @param password   String
     * @param forceLogin boolean
     * @param plugin     AuthMe
     * @param data       DataSource
     */
    public AsynchronousLogin(Player player, String password, boolean forceLogin, AuthMe plugin, DataSource data) {
        this.m = plugin.getMessages();
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.password = password;
        this.realName = player.getName();
        this.forceLogin = forceLogin;
        this.plugin = plugin;
        this.database = data;
    }

    /**
     * Method getIP.
     *
     * @return String
     */
    protected String getIP() {
        return plugin.getIP(player);
    }

    /**
     * Method needsCaptcha.
     *
     * @return boolean
     */
    protected boolean needsCaptcha() {
        if (Settings.useCaptcha) {
            if (!plugin.captcha.containsKey(name)) {
                plugin.captcha.putIfAbsent(name, 1);
            } else {
                int i = plugin.captcha.get(name) + 1;
                plugin.captcha.remove(name);
                plugin.captcha.putIfAbsent(name, i);
            }
            if (plugin.captcha.containsKey(name) && plugin.captcha.get(name) >= Settings.maxLoginTry) {
                plugin.cap.put(name, rdm.nextString());
                for (String s : m.retrieve(MessageKey.USAGE_CAPTCHA)) {
                    player.sendMessage(s.replace("THE_CAPTCHA", plugin.cap.get(name)).replace("<theCaptcha>", plugin.cap.get(name)));
                }
                return true;
            } else if (plugin.captcha.containsKey(name) && plugin.captcha.get(name) >= Settings.maxLoginTry) {
                plugin.captcha.remove(name);
                plugin.cap.remove(name);
            }
        }
        return false;
    }

    /**
     * Checks the precondition for authentication (like user known) and returns
     * the playerAuth-State
     *
     * @return PlayerAuth
     */
    protected PlayerAuth preAuth() {
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return null;
        }
        if (!database.isAuthAvailable(name)) {
            m.send(player, MessageKey.USER_NOT_REGISTERED);
            if (LimboCache.getInstance().hasLimboPlayer(name)) {
                LimboCache.getInstance().getLimboPlayer(name).getMessageTaskId().cancel();
                String[] msg;
                if (Settings.emailRegistration) {
                    msg = m.retrieve(MessageKey.REGISTER_EMAIL_MESSAGE);
                } else {
                    msg = m.retrieve(MessageKey.REGISTER_MESSAGE);
                }
                BukkitTask msgT = Bukkit.getScheduler().runTaskAsynchronously(plugin, new MessageTask(plugin, name, msg, Settings.getWarnMessageInterval));
                LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT);
            }
            return null;
        }
        if (Settings.getMaxLoginPerIp > 0 && !plugin.getPermissionsManager().hasPermission(player, UserPermission.ALLOW_MULTIPLE_ACCOUNTS) && !getIP().equalsIgnoreCase("127.0.0.1") && !getIP().equalsIgnoreCase("localhost")) {
            if (plugin.isLoggedIp(name, getIP())) {
                m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
                return null;
            }
        }
        PlayerAuth pAuth = database.getAuth(name);
        if (pAuth == null) {
            m.send(player, MessageKey.USER_NOT_REGISTERED);
            return null;
        }
        if (!Settings.getMySQLColumnGroup.isEmpty() && pAuth.getGroupId() == Settings.getNonActivatedGroup) {
            m.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
            return null;
        }
        AuthMeAsyncPreLoginEvent event = new AuthMeAsyncPreLoginEvent(player);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.canLogin())
            return null;
        return pAuth;
    }

    public void process() {
        PlayerAuth pAuth = preAuth();
        if (pAuth == null || needsCaptcha())
            return;

        String hash = pAuth.getHash();
        String email = pAuth.getEmail();
        boolean passwordVerified = true;
        if (!forceLogin)
            try {
                passwordVerified = PasswordSecurity.comparePasswordWithHash(password, hash, realName);
            } catch (Exception ex) {
                ConsoleLogger.showError(ex.getMessage());
                m.send(player, MessageKey.ERROR);
                return;
            }
        if (passwordVerified && player.isOnline()) {
            PlayerAuth auth = new PlayerAuth(name, hash, getIP(), new Date().getTime(), email, realName);
            database.updateSession(auth);

            if (Settings.useCaptcha) {
                if (plugin.captcha.containsKey(name)) {
                    plugin.captcha.remove(name);
                }
                if (plugin.cap.containsKey(name)) {
                    plugin.cap.remove(name);
                }
            }

            player.setNoDamageTicks(0);
            if (!forceLogin)
                m.send(player, MessageKey.LOGIN_SUCCESS);

            displayOtherAccounts(auth, player);

            if (Settings.recallEmail) {
                if (email == null || email.isEmpty() || email.equalsIgnoreCase("your@email.com")) {
                    m.send(player, MessageKey.EMAIL_ADDED_SUCCESS);
                }
            }

            if (!Settings.noConsoleSpam) {
                ConsoleLogger.info(realName + " logged in!");
            }

            // makes player isLoggedin via API
            PlayerCache.getInstance().addPlayer(auth);
            database.setLogged(name);
            plugin.otherAccounts.addPlayer(player.getUniqueId());

            // As the scheduling executes the Task most likely after the current
            // task, we schedule it in the end
            // so that we can be sure, and have not to care if it might be
            // processed in other order.
            ProcessSyncronousPlayerLogin syncronousPlayerLogin = new ProcessSyncronousPlayerLogin(player, plugin, database);
            if (syncronousPlayerLogin.getLimbo() != null) {
                if (syncronousPlayerLogin.getLimbo().getTimeoutTaskId() != null)
                    syncronousPlayerLogin.getLimbo().getTimeoutTaskId().cancel();
                if (syncronousPlayerLogin.getLimbo().getMessageTaskId() != null)
                    syncronousPlayerLogin.getLimbo().getMessageTaskId().cancel();
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, syncronousPlayerLogin);
        } else if (player.isOnline()) {
            if (!Settings.noConsoleSpam)
                ConsoleLogger.info(realName + " used the wrong password");
            if (Settings.isKickOnWrongPasswordEnabled) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                    @Override
                    public void run() {
                        if (AuthMePlayerListener.gameMode != null && AuthMePlayerListener.gameMode.containsKey(name)) {
                            player.setGameMode(AuthMePlayerListener.gameMode.get(name));
                        }
                        player.kickPlayer(m.retrieveSingle(MessageKey.WRONG_PASSWORD));
                    }
                });
            } else {
                m.send(player, MessageKey.WRONG_PASSWORD);
            }
        } else {
            ConsoleLogger.showError("Player " + name + " wasn't online during login process, aborted... ");
        }
    }

    /**
     * Method displayOtherAccounts.
     *
     * @param auth PlayerAuth
     * @param p    Player
     */
    public void displayOtherAccounts(PlayerAuth auth, Player p) {
        if (!Settings.displayOtherAccounts) {
            return;
        }
        if (auth == null) {
            return;
        }
        List<String> auths = this.database.getAllAuthsByName(auth);
        // List<String> uuidlist =
        // plugin.otherAccounts.getAllPlayersByUUID(player.getUniqueId());
        if (auths.isEmpty()) {
            return;
        }
        if (auths.size() == 1) {
            return;
        }
        StringBuilder message = new StringBuilder("[AuthMe] ");
        // String uuidaccounts =
        // "[AuthMe] PlayerNames has %size% links to this UUID : ";
        int i = 0;
        for (String account : auths) {
            i++;
            message.append(account);
            if (i != auths.size()) {
                message.append(", ");
            } else {
                message.append('.');
            }
        }
        /*
         * TODO: Active uuid system i = 0; for (String account : uuidlist) {
         * i++; uuidaccounts = uuidaccounts + account; if (i != auths.size()) {
         * uuidaccounts = uuidaccounts + ", "; } else { uuidaccounts =
         * uuidaccounts + "."; } }
         */
        for (Player player : Utils.getOnlinePlayers()) {
            if (plugin.getPermissionsManager().hasPermission(player, UserPermission.SEE_OTHER_ACCOUNTS)) {
                player.sendMessage("[AuthMe] The player " + auth.getNickname() + " has " + auths.size() + " accounts");
                player.sendMessage(message.toString());
                // player.sendMessage(uuidaccounts.replace("%size%",
                // ""+uuidlist.size()));
            }
        }
    }
}
