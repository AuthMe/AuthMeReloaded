package fr.xephi.authme.process.login;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeAsyncPreLoginEvent;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.Date;
import java.util.List;

/**
 */
public class AsynchronousLogin {

    private final Player player;
    private final String name;
    private final String realName;
    private final String password;
    private final boolean forceLogin;
    private final AuthMe plugin;
    private final DataSource database;
    private final Messages m;
    private final String ip;
    private final NewSetting settings;

    /**
     * Constructor for AsynchronousLogin.
     *
     * @param player     Player
     * @param password   String
     * @param forceLogin boolean
     * @param plugin     AuthMe
     * @param data       DataSource
     * @param settings   The settings
     */
    public AsynchronousLogin(Player player, String password, boolean forceLogin, AuthMe plugin, DataSource data,
                             NewSetting settings) {
        this.m = plugin.getMessages();
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.password = password;
        this.realName = player.getName();
        this.forceLogin = forceLogin;
        this.plugin = plugin;
        this.database = data;
        this.ip = plugin.getIP(player);
        this.settings = settings;
    }

    private boolean needsCaptcha() {
        if (Settings.useCaptcha) {
            if (!plugin.captcha.containsKey(name)) {
                plugin.captcha.putIfAbsent(name, 1);
            } else {
                int i = plugin.captcha.get(name) + 1;
                plugin.captcha.remove(name);
                plugin.captcha.putIfAbsent(name, i);
            }
            if (plugin.captcha.containsKey(name) && plugin.captcha.get(name) > Settings.maxLoginTry) {
                plugin.cap.putIfAbsent(name, RandomString.generate(Settings.captchaLength));
                m.send(player, MessageKey.USAGE_CAPTCHA, plugin.cap.get(name));
                return true;
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
    private PlayerAuth preAuth() {
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return null;
        }

        PlayerAuth pAuth = database.getAuth(name);
        if (pAuth == null) {
            m.send(player, MessageKey.USER_NOT_REGISTERED);
            if (LimboCache.getInstance().hasLimboPlayer(name)) {
                LimboCache.getInstance().getLimboPlayer(name).getMessageTaskId().cancel();
                String[] msg;
                if (Settings.emailRegistration) {
                    msg = m.retrieve(MessageKey.REGISTER_EMAIL_MESSAGE);
                } else {
                    msg = m.retrieve(MessageKey.REGISTER_MESSAGE);
                }
                BukkitTask msgT = Bukkit.getScheduler().runTaskAsynchronously(plugin,
                    new MessageTask(plugin, name, msg, settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)));
                LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT);
            }
            return null;
        }

        if (!Settings.getMySQLColumnGroup.isEmpty() && pAuth.getGroupId() == Settings.getNonActivatedGroup) {
            m.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
            return null;
        }

        if (Settings.getMaxLoginPerIp > 0
            && !plugin.getPermissionsManager().hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && !ip.equalsIgnoreCase("127.0.0.1") && !ip.equalsIgnoreCase("localhost")) {
            if (plugin.isLoggedIp(name, ip)) {
                m.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
                return null;
            }
        }

        AuthMeAsyncPreLoginEvent event = new AuthMeAsyncPreLoginEvent(player);
        Bukkit.getServer().getPluginManager().callEvent(event);
        if (!event.canLogin()) {
            return null;
        }
        return pAuth;
    }

    public void process() {
        PlayerAuth pAuth = preAuth();
        if (pAuth == null || needsCaptcha()) {
            return;
        }

        if (pAuth.getIp().equals("127.0.0.1") && !pAuth.getIp().equals(ip)) {
            pAuth.setIp(ip);
            database.saveAuth(pAuth);
        }

        String email = pAuth.getEmail();
        boolean passwordVerified = forceLogin || plugin.getPasswordSecurity()
            .comparePassword(password, pAuth.getPassword(), realName);

        if (passwordVerified && player.isOnline()) {
            PlayerAuth auth = PlayerAuth.builder()
                .name(name)
                .realName(realName)
                .ip(ip)
                .email(email)
                .password(pAuth.getPassword())
                .build();
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

            displayOtherAccounts(auth);

            if (Settings.recallEmail && (StringUtils.isEmpty(email) || "your@email.com".equalsIgnoreCase(email))) {
                m.send(player, MessageKey.EMAIL_ADDED_SUCCESS);
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
            ProcessSyncPlayerLogin syncPlayerLogin = new ProcessSyncPlayerLogin(player, plugin, database, settings);
            if (syncPlayerLogin.getLimbo() != null) {
                if (syncPlayerLogin.getLimbo().getTimeoutTaskId() != null) {
                    syncPlayerLogin.getLimbo().getTimeoutTaskId().cancel();
                }
                if (syncPlayerLogin.getLimbo().getMessageTaskId() != null) {
                    syncPlayerLogin.getLimbo().getMessageTaskId().cancel();
                }
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, syncPlayerLogin);
        } else if (player.isOnline()) {
            if (!Settings.noConsoleSpam)
                ConsoleLogger.info(realName + " used the wrong password");
            if (Settings.isKickOnWrongPasswordEnabled) {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                    @Override
                    public void run() {
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

    public void displayOtherAccounts(PlayerAuth auth) {
        if (!Settings.displayOtherAccounts || auth == null) {
            return;
        }

        List<String> auths = this.database.getAllAuthsByName(auth);
        if (auths.size() < 2) {
            return;
        }
        String message = "[AuthMe] " + StringUtils.join(", ", auths) + ".";
        for (Player player : Utils.getOnlinePlayers()) {
            if (plugin.getPermissionsManager().hasPermission(player, AdminPermission.SEE_OTHER_ACCOUNTS)
            		|| (player.getName().equals(this.player.getName())
            				&& plugin.getPermissionsManager().hasPermission(player, PlayerPermission.SEE_OWN_ACCOUNTS))) {
                player.sendMessage("[AuthMe] The player " + auth.getNickname() + " has " + auths.size() + " accounts");
                player.sendMessage(message);
            }
        }
    }
}
