package fr.xephi.authme.process.login;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.TempbanManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.captcha.LoginCaptchaManager;
import fr.xephi.authme.data.limbo.LimboMessageType;
import fr.xephi.authme.data.limbo.LimboPlayerState;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeAsyncPreLoginEvent;
import fr.xephi.authme.events.FailedLoginEvent;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.SyncProcessManager;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.service.bungeecord.MessageType;
import fr.xephi.authme.settings.properties.DatabaseSettings;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * Asynchronous task for a player login.
 */
public class AsynchronousLogin implements AsynchronousProcess {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private SyncProcessManager syncProcessManager;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private LoginCaptchaManager loginCaptchaManager;

    @Inject
    private TempbanManager tempbanManager;

    @Inject
    private LimboService limboService;

    @Inject
    private EmailService emailService;

    @Inject
    private SessionService sessionService;

    @Inject
    private BungeeSender bungeeSender;

    AsynchronousLogin() {
    }

    /**
     * Processes a player's login request.
     *
     * @param player the player to log in
     * @param password the password to log in with
     */
    public void login(Player player, String password) {
        PlayerAuth auth = getPlayerAuth(player);
        if (auth != null && checkPlayerInfo(player, auth, password)) {
            if (auth.getTotpKey() != null) {
                limboService.resetMessageTask(player, LimboMessageType.TOTP_CODE);
                limboService.getLimboPlayer(player.getName()).setState(LimboPlayerState.TOTP_REQUIRED);
                // TODO #1141: Check if we should check limbo state before processing password
            } else {
                performLogin(player, auth);
            }
        }
    }

    /**
     * Logs a player in without requiring a password.
     *
     * @param player the player to log in
     */
    public void forceLogin(Player player) {
        PlayerAuth auth = getPlayerAuth(player);
        if (auth != null) {
            performLogin(player, auth);
        }
    }

    /**
     * Checks the precondition for authentication (like user known) and returns
     * the player's {@link PlayerAuth} object.
     *
     * @param player the player to check
     * @return the PlayerAuth object, or {@code null} if the player doesn't exist or may not log in
     *         (e.g. because he is already logged in)
     */
    private PlayerAuth getPlayerAuth(Player player) {
        final String name = player.getName().toLowerCase();
        if (playerCache.isAuthenticated(name)) {
            service.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return null;
        }

        PlayerAuth auth = dataSource.getAuth(name);
        if (auth == null) {
            service.send(player, MessageKey.UNKNOWN_USER);
            // Recreate the message task to immediately send the message again as response
            limboService.resetMessageTask(player, LimboMessageType.REGISTER);
            return null;
        }

        if (!service.getProperty(DatabaseSettings.MYSQL_COL_GROUP).isEmpty()
            && auth.getGroupId() == service.getProperty(HooksSettings.NON_ACTIVATED_USERS_GROUP)) {
            service.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
            return null;
        }

        final String ip = PlayerUtils.getPlayerIp(player);
        if (hasReachedMaxLoggedInPlayersForIp(player, ip)) {
            service.send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
            return null;
        }

        boolean isAsync = service.getProperty(PluginSettings.USE_ASYNC_TASKS);
        AuthMeAsyncPreLoginEvent event = new AuthMeAsyncPreLoginEvent(player, isAsync);
        bukkitService.callEvent(event);
        if (!event.canLogin()) {
            return null;
        }
        return auth;
    }

    /**
     * Checks various conditions for regular player login (not used in force login).
     *
     * @param player the player requesting to log in
     * @param auth the PlayerAuth object of the player
     * @param password the password supplied by the player
     * @return true if the password matches and all other conditions are met (e.g. no captcha required),
     *         false otherwise
     */
    private boolean checkPlayerInfo(Player player, PlayerAuth auth, String password) {
        final String name = player.getName().toLowerCase();

        // If captcha is required send a message to the player and deny to log in
        if (loginCaptchaManager.isCaptchaRequired(name)) {
            service.send(player, MessageKey.USAGE_CAPTCHA, loginCaptchaManager.getCaptchaCodeOrGenerateNew(name));
            return false;
        }

        final String ip = PlayerUtils.getPlayerIp(player);

        // Increase the counts here before knowing the result of the login.
        loginCaptchaManager.increaseLoginFailureCount(name);
        tempbanManager.increaseCount(ip, name);

        if (passwordSecurity.comparePassword(password, auth.getPassword(), player.getName())) {
            return true;
        } else {
            handleWrongPassword(player, auth, ip);
            return false;
        }
    }

    /**
     * Handles a login with wrong password.
     *
     * @param player the player who attempted to log in
     * @param auth the PlayerAuth object of the player
     * @param ip the ip address of the player
     */
    private void handleWrongPassword(Player player, PlayerAuth auth, String ip) {
        ConsoleLogger.fine(player.getName() + " used the wrong password");

        bukkitService.createAndCallEvent(isAsync -> new FailedLoginEvent(player, isAsync));
        if (tempbanManager.shouldTempban(ip)) {
            tempbanManager.tempbanPlayer(player);
        } else if (service.getProperty(RestrictionSettings.KICK_ON_WRONG_PASSWORD)) {
            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(
                () -> player.kickPlayer(service.retrieveSingleMessage(player, MessageKey.WRONG_PASSWORD)));
        } else {
            service.send(player, MessageKey.WRONG_PASSWORD);

            // If the authentication fails check if Captcha is required and send a message to the player
            if (loginCaptchaManager.isCaptchaRequired(player.getName())) {
                limboService.muteMessageTask(player);
                service.send(player, MessageKey.USAGE_CAPTCHA,
                    loginCaptchaManager.getCaptchaCodeOrGenerateNew(player.getName()));
            } else if (emailService.hasAllInformation() && !Utils.isEmailEmpty(auth.getEmail())) {
                service.send(player, MessageKey.FORGOT_PASSWORD_MESSAGE);
            }
        }
    }

    /**
     * Sets the player to the logged in state.
     *
     * @param player the player to log in
     * @param auth the associated PlayerAuth object
     */
    public void performLogin(Player player, PlayerAuth auth) {
        if (player.isOnline()) {
            final boolean isFirstLogin = (auth.getLastLogin() == null);

            // Update auth to reflect this new login
            final String ip = PlayerUtils.getPlayerIp(player);
            auth.setRealName(player.getName());
            auth.setLastLogin(System.currentTimeMillis());
            auth.setLastIp(ip);
            dataSource.updateSession(auth);
            bungeeSender.sendAuthMeBungeecordMessage(MessageType.REFRESH_SESSION, player.getName());

            // Successful login, so reset the captcha & temp ban count
            final String name = player.getName();
            loginCaptchaManager.resetLoginFailureCount(name);
            tempbanManager.resetCount(ip, name);
            player.setNoDamageTicks(0);

            service.send(player, MessageKey.LOGIN_SUCCESS);

            // Other auths
            List<String> auths = dataSource.getAllAuthsByIp(auth.getLastIp());
            displayOtherAccounts(auths, player);

            final String email = auth.getEmail();
            if (service.getProperty(EmailSettings.RECALL_PLAYERS) && Utils.isEmailEmpty(email)) {
                service.send(player, MessageKey.ADD_EMAIL_MESSAGE);
            }

            ConsoleLogger.fine(player.getName() + " logged in!");

            // makes player loggedin
            playerCache.updatePlayer(auth);
            dataSource.setLogged(name);
            sessionService.grantSession(name);
            bungeeSender.sendAuthMeBungeecordMessage(MessageType.LOGIN, name);

            // As the scheduling executes the Task most likely after the current
            // task, we schedule it in the end
            // so that we can be sure, and have not to care if it might be
            // processed in other order.
            syncProcessManager.processSyncPlayerLogin(player, isFirstLogin, auths);
        } else {
            ConsoleLogger.warning("Player '" + player.getName() + "' wasn't online during login process, aborted...");
        }
    }

    /**
     * Sends info about the other accounts owned by the given player to the configured users.
     *
     * @param auths the names of the accounts also owned by the player
     * @param player the player
     */
    private void displayOtherAccounts(List<String> auths, Player player) {
        if (!service.getProperty(RestrictionSettings.DISPLAY_OTHER_ACCOUNTS) || auths.size() <= 1) {
            return;
        }

        List<String> formattedNames = new ArrayList<>(auths.size());
        for (String currentName : auths) {
            Player currentPlayer = bukkitService.getPlayerExact(currentName);
            if (currentPlayer != null && currentPlayer.isOnline()) {
                formattedNames.add(ChatColor.GREEN + currentPlayer.getName() + ChatColor.GRAY);
            } else {
                formattedNames.add(currentName);
            }
        }

        String message = ChatColor.GRAY + String.join(", ", formattedNames) + ".";

        ConsoleLogger.fine("The user " + player.getName() + " has " + auths.size() + " accounts:");
        ConsoleLogger.fine(message);

        for (Player onlinePlayer : bukkitService.getOnlinePlayers()) {
            if (onlinePlayer.getName().equalsIgnoreCase(player.getName())
                && service.hasPermission(onlinePlayer, PlayerPermission.SEE_OWN_ACCOUNTS)) {
                service.send(onlinePlayer, MessageKey.ACCOUNTS_OWNED_SELF, Integer.toString(auths.size()));
                onlinePlayer.sendMessage(message);
            } else if (service.hasPermission(onlinePlayer, AdminPermission.SEE_OTHER_ACCOUNTS)) {
                service.send(onlinePlayer, MessageKey.ACCOUNTS_OWNED_OTHER,
                    player.getName(), Integer.toString(auths.size()));
                onlinePlayer.sendMessage(message);
            }
        }
    }

    /**
     * Checks whether the maximum threshold of logged in player per IP address has been reached
     * for the given player and IP address.
     *
     * @param player the player to process
     * @param ip the associated ip address
     * @return true if the threshold has been reached, false otherwise
     */
    @VisibleForTesting
    boolean hasReachedMaxLoggedInPlayersForIp(Player player, String ip) {
        // Do not perform the check if player has multiple accounts permission or if IP is localhost
        if (service.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP) <= 0
            || service.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            || "127.0.0.1".equalsIgnoreCase(ip)
            || "localhost".equalsIgnoreCase(ip)) {
            return false;
        }

        // Count logged in players with same IP address
        final String name = player.getName();
        int count = 0;
        for (Player onlinePlayer : bukkitService.getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(PlayerUtils.getPlayerIp(onlinePlayer))
                && !onlinePlayer.getName().equals(name)
                && dataSource.isLogged(onlinePlayer.getName().toLowerCase())) {
                ++count;
            }
        }
        return count >= service.getProperty(RestrictionSettings.MAX_LOGIN_PER_IP);
    }
}
