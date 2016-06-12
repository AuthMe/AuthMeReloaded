package fr.xephi.authme.process.login;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.CaptchaManager;
import fr.xephi.authme.cache.TempbanManager;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeAsyncPreLoginEvent;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
import java.util.List;

/**
 */
public class AsynchronousLogin implements AsynchronousProcess {

    @Inject
    private AuthMe plugin;

    @Inject
    private DataSource database;

    @Inject
    private ProcessService service;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private LimboCache limboCache;

    @Inject
    private SyncProcessManager syncProcessManager;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private CaptchaManager captchaManager;

    @Inject
    private TempbanManager tempbanManager;

    AsynchronousLogin() { }

    /**
     * Queries the {@link fr.xephi.authme.cache.CaptchaManager} to
     * see if a captcha needs to be entered in order to log in.
     *
     * @param player The player to check
     * @return True if a captcha needs to be entered
     */
    private boolean needsCaptcha(Player player) {
        final String playerName = player.getName();

        return captchaManager.isCaptchaRequired(playerName);
    }

    /**
     * Queries the {@link fr.xephi.authme.cache.TempbanManager} to
     * see if the player has reached the tempban threshold.
     *
     * @param player The player to check
     * @return True if the player needs to be tempbanned
     */
    private boolean shouldTempban(Player player) {
        final String playerName = player.getName();

        return tempbanManager.shouldTempban(playerName);
    }

    /**
     * Checks the precondition for authentication (like user known) and returns
     * the playerAuth-State
     *
     * @return PlayerAuth
     */
    private PlayerAuth preAuth(Player player) {
        final String name = player.getName().toLowerCase();
        if (playerCache.isAuthenticated(name)) {
            service.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return null;
        }

        PlayerAuth pAuth = database.getAuth(name);
        if (pAuth == null) {
            service.send(player, MessageKey.USER_NOT_REGISTERED);

            LimboPlayer limboPlayer = limboCache.getLimboPlayer(name);
            if (limboPlayer != null) {
                limboPlayer.getMessageTask().cancel();
                String[] msg = service.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)
                    ? service.retrieveMessage(MessageKey.REGISTER_EMAIL_MESSAGE)
                    : service.retrieveMessage(MessageKey.REGISTER_MESSAGE);
                BukkitTask messageTask = bukkitService.runTask(new MessageTask(bukkitService,
                    name, msg, service.getProperty(RegistrationSettings.MESSAGE_INTERVAL)));
                limboPlayer.setMessageTask(messageTask);
            }
            return null;
        }

        if (!service.getProperty(DatabaseSettings.MYSQL_COL_GROUP).isEmpty() && pAuth.getGroupId() == Settings.getNonActivatedGroup) {
            service.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
            return null;
        }

        final String ip = Utils.getPlayerIp(player);
        if (service.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP) > 0
            && !permissionsManager.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && !"127.0.0.1".equalsIgnoreCase(ip) && !"localhost".equalsIgnoreCase(ip)) {
            if (isLoggedIp(name, ip)) {
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

    public void login(final Player player, String password, boolean forceLogin) {
        PlayerAuth pAuth = preAuth(player);
        if (pAuth == null || needsCaptcha(player)) {
            return;
        }

        final String name = player.getName().toLowerCase();
        final String ip = Utils.getPlayerIp(player);

        // Increase the counts here before knowing the result of the login.
        // If the login is successful, we clear the count for the player.
        captchaManager.increaseCount(name);
        tempbanManager.increaseCount(name);

        if ("127.0.0.1".equals(pAuth.getIp()) && !pAuth.getIp().equals(ip)) {
            pAuth.setIp(ip);
            database.updateIp(pAuth.getNickname(), ip);
        }

        String email = pAuth.getEmail();
        boolean passwordVerified = forceLogin || passwordSecurity.comparePassword(
            password, pAuth.getPassword(), player.getName());
        if (passwordVerified && player.isOnline()) {
            PlayerAuth auth = PlayerAuth.builder()
                .name(name)
                .realName(player.getName())
                .ip(ip)
                .email(email)
                .password(pAuth.getPassword())
                .build();
            database.updateSession(auth);

            captchaManager.resetCounts(name);
            tempbanManager.resetCount(name);
            player.setNoDamageTicks(0);

            if (!forceLogin)
                service.send(player, MessageKey.LOGIN_SUCCESS);

            displayOtherAccounts(auth, player);

            if (service.getProperty(EmailSettings.RECALL_PLAYERS)
                && (StringUtils.isEmpty(email) || "your@email.com".equalsIgnoreCase(email))) {
                service.send(player, MessageKey.ADD_EMAIL_MESSAGE);
            }

            if (!service.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
                ConsoleLogger.info(player.getName() + " logged in!");
            }

            // makes player isLoggedin via API
            playerCache.addPlayer(auth);
            database.setLogged(name);

            // As the scheduling executes the Task most likely after the current
            // task, we schedule it in the end
            // so that we can be sure, and have not to care if it might be
            // processed in other order.
            LimboPlayer limboPlayer = limboCache.getLimboPlayer(name);
            if (limboPlayer != null) {
                if (limboPlayer.getTimeoutTask() != null) {
                    limboPlayer.getTimeoutTask().cancel();
                }
                if (limboPlayer.getMessageTask() != null) {
                    limboPlayer.getMessageTask().cancel();
                }
            }
            syncProcessManager.processSyncPlayerLogin(player);
        } else if (player.isOnline()) {
            if (!service.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
                ConsoleLogger.info(player.getName() + " used the wrong password");
            }
            if (service.getProperty(RestrictionSettings.KICK_ON_WRONG_PASSWORD)) {
                bukkitService.scheduleSyncDelayedTask(new Runnable() {
                    @Override
                    public void run() {
                        player.kickPlayer(service.retrieveSingleMessage(MessageKey.WRONG_PASSWORD));
                    }
                });
            } else if (shouldTempban(player)) {
                tempbanManager.tempbanPlayer(player);
            } else  {
                service.send(player, MessageKey.WRONG_PASSWORD);

                // Check again if a captcha is required to log in
                if (needsCaptcha(player)) {
                    service.send(player, MessageKey.USAGE_CAPTCHA, captchaManager.getCaptchaCodeOrGenerateNew(name));
                }
            }
        } else {
            ConsoleLogger.showError("Player " + name + " wasn't online during login process, aborted... ");
        }
    }

    // TODO #423: allow translation!
    private void displayOtherAccounts(PlayerAuth auth, Player player) {
        if (!service.getProperty(RestrictionSettings.DISPLAY_OTHER_ACCOUNTS) || auth == null) {
            return;
        }

        List<String> auths = database.getAllAuthsByIp(auth.getIp());
        if (auths.size() < 2) {
            return;
        }
        // TODO #423: color player names with green if the account is online
        String message = StringUtils.join(", ", auths) + ".";

        ConsoleLogger.info("The user " + player.getName() + " has " + auths.size() + " accounts:");
        ConsoleLogger.info(message);

        for (Player onlinePlayer : bukkitService.getOnlinePlayers()) {
            if (onlinePlayer.getName().equalsIgnoreCase(player.getName())
                && permissionsManager.hasPermission(onlinePlayer, PlayerPermission.SEE_OWN_ACCOUNTS)) {
                onlinePlayer.sendMessage("You own " + auths.size() + " accounts:");
                onlinePlayer.sendMessage(message);
            } else if (permissionsManager.hasPermission(onlinePlayer, AdminPermission.SEE_OTHER_ACCOUNTS)) {
                onlinePlayer.sendMessage("The user " + player.getName() + " has " + auths.size() + " accounts:");
                onlinePlayer.sendMessage(message);
            }
        }
    }

    private boolean isLoggedIp(String name, String ip) {
        int count = 0;
        for (Player player : bukkitService.getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(Utils.getPlayerIp(player))
                && database.isLogged(player.getName().toLowerCase())
                && !player.getName().equalsIgnoreCase(name)) {
                ++count;
            }
        }
        return count >= service.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP);
    }
}
