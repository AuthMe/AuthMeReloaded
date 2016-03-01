package fr.xephi.authme.process.login;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeAsyncPreLoginEvent;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 */
public class AsynchronousLogin implements Process {

    private final Player player;
    private final String name;
    private final String realName;
    private final String password;
    private final boolean forceLogin;
    private final AuthMe plugin;
    private final DataSource database;
    private final String ip;
    private final ProcessService service;

    public AsynchronousLogin(Player player, String password, boolean forceLogin, AuthMe plugin, DataSource data,
                             ProcessService service) {
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.password = password;
        this.realName = player.getName();
        this.forceLogin = forceLogin;
        this.plugin = plugin;
        this.database = data;
        this.ip = service.getIpAddressManager().getPlayerIp(player);
        this.service = service;
    }

    private boolean needsCaptcha() {
        if (service.getProperty(SecuritySettings.USE_CAPTCHA)) {
            if (!plugin.captcha.containsKey(name)) {
                plugin.captcha.putIfAbsent(name, 1);
            } else {
                int i = plugin.captcha.get(name) + 1;
                plugin.captcha.remove(name);
                plugin.captcha.putIfAbsent(name, i);
            }
            if (plugin.captcha.containsKey(name) && plugin.captcha.get(name) > Settings.maxLoginTry) {
                plugin.cap.putIfAbsent(name, RandomString.generate(Settings.captchaLength));
                service.send(player, MessageKey.USAGE_CAPTCHA, plugin.cap.get(name));
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
            service.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return null;
        }

        PlayerAuth pAuth = database.getAuth(name);
        if (pAuth == null) {
            service.send(player, MessageKey.USER_NOT_REGISTERED);
            if (LimboCache.getInstance().hasLimboPlayer(name)) {
                LimboCache.getInstance().getLimboPlayer(name).getMessageTaskId().cancel();
                String[] msg = service.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)
                    ? service.retrieveMessage(MessageKey.REGISTER_EMAIL_MESSAGE)
                    : service.retrieveMessage(MessageKey.REGISTER_MESSAGE);
                BukkitTask messageTask = service.runTask(
                    new MessageTask(plugin, name, msg, service.getProperty(RegistrationSettings.MESSAGE_INTERVAL)));
                LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(messageTask);
            }
            return null;
        }

        if (!Settings.getMySQLColumnGroup.isEmpty() && pAuth.getGroupId() == Settings.getNonActivatedGroup) {
            service.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
            return null;
        }

        if (Settings.getMaxLoginPerIp > 0
            && !plugin.getPermissionsManager().hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && !ip.equalsIgnoreCase("127.0.0.1") && !ip.equalsIgnoreCase("localhost")) {
            if (plugin.isLoggedIp(name, ip)) {
                service.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
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

    @Override
    public void run() {
        PlayerAuth pAuth = preAuth();
        if (pAuth == null || needsCaptcha()) {
            return;
        }

        if (pAuth.getIp().equals("127.0.0.1") && !pAuth.getIp().equals(ip)) {
            pAuth.setIp(ip);
            database.updateIp(pAuth.getNickname(), ip);
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
                service.send(player, MessageKey.LOGIN_SUCCESS);

            displayOtherAccounts(auth);

            if (Settings.recallEmail && (StringUtils.isEmpty(email) || "your@email.com".equalsIgnoreCase(email))) {
                service.send(player, MessageKey.ADD_EMAIL_MESSAGE);
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
            ProcessSyncPlayerLogin syncPlayerLogin = new ProcessSyncPlayerLogin(
                player, plugin, database, service.getSettings());
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
                        player.kickPlayer(service.retrieveSingleMessage(MessageKey.WRONG_PASSWORD));
                    }
                });
            } else {
                service.send(player, MessageKey.WRONG_PASSWORD);
            }
        } else {
            ConsoleLogger.showError("Player " + name + " wasn't online during login process, aborted... ");
        }
    }

    private void displayOtherAccounts(PlayerAuth auth) {
        if (!Settings.displayOtherAccounts || auth == null) {
            return;
        }

        List<String> auths = this.database.getAllAuthsByIp(auth.getIp());
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
