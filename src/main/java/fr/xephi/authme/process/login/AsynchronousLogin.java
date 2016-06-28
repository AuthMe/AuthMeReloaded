package fr.xephi.authme.process.login;

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
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.LimboPlayerTaskManager;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class AsynchronousLogin implements AsynchronousProcess {

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

    @Inject
    private LimboPlayerTaskManager limboPlayerTaskManager;

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

            // TODO ljacqu 20160612: Why is the message task being canceled and added again here?
            limboPlayerTaskManager.registerMessageTask(name, false);
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
        if (pAuth == null) {
            return;
        }

        final String name = player.getName().toLowerCase();

        // If Captcha is required send a message to the player and deny to login
        if (needsCaptcha(player)) {
            service.send(player, MessageKey.USAGE_CAPTCHA, captchaManager.getCaptchaCodeOrGenerateNew(name));
            return;
        }

        final String ip = Utils.getPlayerIp(player);

        // Increase the counts here before knowing the result of the login.
        // If the login is successful, we clear the captcha count for the player.
        captchaManager.increaseCount(name);
        tempbanManager.increaseCount(ip);

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
                limboPlayer.clearTasks();
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
            } else if (tempbanManager.shouldTempban(ip)) {
                tempbanManager.tempbanPlayer(player);
            } else  {
                service.send(player, MessageKey.WRONG_PASSWORD);

                // If the authentication fails check if Captcha is required and send a message to the player
                if (needsCaptcha(player)) {
                    service.send(player, MessageKey.USAGE_CAPTCHA, captchaManager.getCaptchaCodeOrGenerateNew(name));
                }
            }
        } else {
            ConsoleLogger.showError("Player " + name + " wasn't online during login process, aborted... ");
        }
    }

    private void displayOtherAccounts(PlayerAuth auth, Player player) {
        if (!service.getProperty(RestrictionSettings.DISPLAY_OTHER_ACCOUNTS) || auth == null) {
            return;
        }

        List<String> auths = database.getAllAuthsByIp(auth.getIp());
        if (auths.size() <= 1) {
            return;
        }

        List<String> formattedNames = new ArrayList<>(auths.size());
        for (String currentName : auths) {
            Player currentPlayer = bukkitService.getPlayerExact(currentName);
            if (currentPlayer != null && currentPlayer.isOnline()) {
                formattedNames.add(ChatColor.GREEN + currentName);
            } else {
                formattedNames.add(currentName);
            }
        }

        String message = ChatColor.GRAY + StringUtils.join(ChatColor.GRAY + ", ", formattedNames) + ".";

        if (!service.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
            ConsoleLogger.info("The user " + player.getName() + " has " + auths.size() + " accounts:");
            ConsoleLogger.info(message);
        }

        for (Player onlinePlayer : bukkitService.getOnlinePlayers()) {
            if (onlinePlayer.getName().equalsIgnoreCase(player.getName())
                && permissionsManager.hasPermission(onlinePlayer, PlayerPermission.SEE_OWN_ACCOUNTS)) {
                service.send(onlinePlayer, MessageKey.ACCOUNTS_OWNED_SELF, Integer.toString(auths.size()));
                onlinePlayer.sendMessage(message);
            } else if (permissionsManager.hasPermission(onlinePlayer, AdminPermission.SEE_OTHER_ACCOUNTS)) {
                service.send(onlinePlayer, MessageKey.ACCOUNTS_OWNED_OTHER,
                    player.getName(), Integer.toString(auths.size()));
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
