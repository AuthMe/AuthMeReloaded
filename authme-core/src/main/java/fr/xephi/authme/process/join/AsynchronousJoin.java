package fr.xephi.authme.process.join;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.platform.DialogAdapter;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.process.register.AsyncRegister;
import fr.xephi.authme.process.register.executors.EmailRegisterParams;
import fr.xephi.authme.process.register.executors.PasswordRegisterParams;
import fr.xephi.authme.process.register.executors.RegistrationMethod;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.DialogStateService;
import fr.xephi.authme.service.DialogWindowService;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PreJoinDialogService;
import fr.xephi.authme.service.PremiumLoginVerifier;
import fr.xephi.authme.service.PremiumService;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.service.ProxyLoginRequestValidator;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.service.bungeecord.MessageType;
import fr.xephi.authme.settings.WelcomeMessageConfiguration;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.service.PasswordRecoveryService;
import fr.xephi.authme.service.RecoveryCodeService;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PremiumSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.InternetProtocolUtils;
import fr.xephi.authme.util.PlayerUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Locale;
import java.util.UUID;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;
import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;

/**
 * Asynchronous process for when a player joins.
 */
public class AsynchronousJoin implements AsynchronousProcess {
    
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(AsynchronousJoin.class);

    @Inject
    private Server server;

    @Inject
    private DataSource database;

    @Inject
    private CommonService service;

    @Inject
    private LimboService limboService;

    @Inject
    private PluginHookService pluginHookService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private AsynchronousLogin asynchronousLogin;

    @Inject
    private AsyncRegister asyncRegister;

    @Inject
    private CommandManager commandManager;

    @Inject
    private ValidationService validationService;

    @Inject
    private WelcomeMessageConfiguration welcomeMessageConfiguration;

    @Inject
    private SessionService sessionService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private BungeeSender bungeeSender;

    @Inject
    private ProxySessionManager proxySessionManager;

    @Inject
    private DialogAdapter dialogAdapter;

    @Inject
    private DialogWindowService dialogWindowService;

    @Inject
    private DialogStateService dialogStateService;

    @Inject
    private PreJoinDialogService preJoinDialogService;

    @Inject
    private PremiumLoginVerifier premiumLoginVerifier;

    @Inject
    private PendingPremiumCache pendingPremiumCache;

    @Inject
    private PremiumService premiumService;

    @Inject
    private ProxyLoginRequestValidator proxyLoginRequestValidator;

    @Inject
    private EmailService emailService;

    @Inject
    private PasswordRecoveryService passwordRecoveryService;

    @Inject
    private RecoveryCodeService recoveryCodeService;

    AsynchronousJoin() {
    }

    /**
     * Processes the given player that has just joined.
     *
     * @param player the player to process
     */
    public void processJoin(Player player) {
        String name = player.getName().toLowerCase(Locale.ROOT);
        String ip = PlayerUtils.getPlayerIp(player);
        UUID playerId = player.getUniqueId();
        String pendingLoginPassword = preJoinDialogService.consumePendingLoginPassword(playerId);
        String pendingRecoveryEmail = preJoinDialogService.consumePendingRecoveryEmail(playerId);
        PreJoinDialogService.PendingRegistration pendingRegistration =
            preJoinDialogService.consumePendingRegistration(playerId);
        boolean shouldSkipPostJoinDialog = preJoinDialogService.consumeSkipPostJoinDialog(playerId);
        boolean pendingForceLogin = preJoinDialogService.consumePendingForceLogin(playerId);
        String pendingKick = preJoinDialogService.consumePendingKickMessage(playerId);
        // pendingKick is applied below, after proxy/premium/session checks, which take priority:
        // a Velocity perform.login arriving just after the player cancelled the pre-join dialog
        // must win over the dialog cancel kick.

        if (!validationService.fulfillsNameRestrictions(player)) {
            handlePlayerWithUnmetNameRestriction(player, ip);
            return;
        }

        if (service.getProperty(RestrictionSettings.UNRESTRICTED_NAMES).contains(name)) {
            return;
        }

        if (service.getProperty(RestrictionSettings.FORCE_SURVIVAL_MODE)
            && player.getGameMode() != GameMode.SURVIVAL
            && !service.hasPermission(player, PlayerStatePermission.BYPASS_FORCE_SURVIVAL)) {
            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, () -> player.setGameMode(GameMode.SURVIVAL));
        }

        if (service.getProperty(HooksSettings.DISABLE_SOCIAL_SPY)) {
            pluginHookService.setEssentialsSocialSpyStatus(player, false);
        }

        if (!validatePlayerCountForIp(player, ip)) {
            return;
        }

        boolean isAuthAvailable = database.isAuthAvailable(name);

        if (isAuthAvailable) {
            // Protect inventory
            if (service.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN)) {
                ProtectInventoryEvent ev = bukkitService.createAndCallEvent(
                    isAsync -> new ProtectInventoryEvent(player, isAsync));
                if (ev.isCancelled()) {
                    bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, player::updateInventory);
                    logger.fine("ProtectInventoryEvent has been cancelled for " + player.getName() + "...");
                }
            }

            // Premium players are always verified by Mojang — skip the session mechanism entirely,
            // which would send a misleading SESSION_RECONNECTION message and run session commands.
            if (canBypassWithPremium(player)) {
                // Premium bypass: player has a verified Mojang UUID matching the connecting player's UUID
                if (bungeeSender.isEnabled()) {
                    // Proxy handles routing; do not attempt backend-side BungeeCord redirect
                    bukkitService.runTaskOptionallyAsync(() -> asynchronousLogin.forceLoginFromProxy(player));
                } else {
                    bukkitService.runTaskOptionallyAsync(() -> asynchronousLogin.forceLogin(player));
                }
                return;
            } else {
                ProxySessionManager.ProxyLoginRequest proxyLoginRequest = proxySessionManager.consumeLoginRequest(name);
                if (proxyLoginRequest != null) {
                    if (!proxyLoginRequestValidator.validate(player, proxyLoginRequest.verifiedPremiumUuid())) {
                        return;
                    }
                    if (playerCache.isAuthenticated(name)) {
                        return;
                    }
                    service.send(player, MessageKey.SESSION_RECONNECTION);
                    // Run commands
                    bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player,
                        () -> commandManager.runCommandsOnSessionLogin(player));
                    // Use forceLoginFromProxy (quiet=true, no BungeeCord redirect) so that if
                    // BungeeReceiver.performLogin() concurrently already completed the login, this
                    // call is a no-op rather than sending an "already logged in" error.
                    bukkitService.runTaskOptionallyAsync(() -> asynchronousLogin.forceLoginFromProxy(player));
                    logger.info("The user " + player.getName() + " has been automatically logged in, "
                        + "as present in autologin queue.");
                    return;
                }
            }
            if (sessionService.canResumeSession(player)) {
                service.send(player, MessageKey.SESSION_RECONNECTION);
                // Run commands
                bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player,
                    () -> commandManager.runCommandsOnSessionLogin(player));
                bukkitService.runTaskOptionallyAsync(() -> asynchronousLogin.forceLogin(player));
                return;
            }
        } else if (!service.getProperty(RegistrationSettings.FORCE) && pendingRegistration == null) {
            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, () -> {
                welcomeMessageConfiguration.sendWelcomeMessage(player);
            });

            // Skip if registration is optional

            if (bungeeSender.isEnabled()) {
                // As described at https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/
                // "Keep in mind that you can't send plugin messages directly after a player joins."
                bukkitService.scheduleSyncDelayedTask(player, () ->
                    bungeeSender.sendAuthMeBungeecordMessage(player, MessageType.LOGIN), 5L);
            }
            return;
        }

        // Guard: a proxy-initiated forceLoginFromProxy() may have already authenticated the player
        // (if the perform.login message arrived and was processed before this async task completes).
        // Scheduling a limbo in that case would freeze the player permanently.
        if (playerCache.isAuthenticated(name)) {
            return;
        }

        // Apply the pre-join dialog cancel kick here, after proxy/premium/session checks above have
        // had a chance to take priority and return early. If perform.login arrived in time, the
        // player was already auto-logged in above and pendingKick is silently discarded.
        if (pendingKick != null) {
            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, () -> player.kickPlayer(pendingKick));
            return;
        }

        processJoinSync(player, isAuthAvailable, pendingLoginPassword, pendingRecoveryEmail, pendingRegistration, shouldSkipPostJoinDialog, pendingForceLogin);
    }

    private void handlePlayerWithUnmetNameRestriction(Player player, String ip) {
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player,
            () -> player.kickPlayer(service.retrieveSingleMessage(player, MessageKey.NOT_OWNER_ERROR)));
        if (service.getProperty(RestrictionSettings.BAN_UNKNOWN_IP)) {
            bukkitService.runOnGlobalRegion(() -> server.banIP(ip));
        }
    }

    /**
     * Performs various operations in sync mode for an unauthenticated player (such as blindness effect and
     * limbo player creation).
     *
     * @param player the player to process
     * @param isAuthAvailable true if the player is registered, false otherwise
     */
    private void processJoinSync(Player player, boolean isAuthAvailable, String pendingLoginPassword,
                                 String pendingRecoveryEmail,
                                 PreJoinDialogService.PendingRegistration pendingRegistration,
                                 boolean shouldSkipPostJoinDialog, boolean pendingForceLogin) {
        int registrationTimeout = service.getProperty(
            isAuthAvailable ? RestrictionSettings.LOGIN_TIMEOUT : RestrictionSettings.REGISTER_TIMEOUT
        ) * TICKS_PER_SECOND;

        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, () -> {
            // Guard: proxy login may have completed between when this task was scheduled and now
            if (playerCache.isAuthenticated(player.getName())) {
                return;
            }
            limboService.createLimboPlayer(player, isAuthAvailable);

            player.setNoDamageTicks(registrationTimeout);
            if (pluginHookService.isEssentialsAvailable() && service.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)) {
                player.performCommand("motd");
            }
            if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
                // Allow infinite blindness effect
                int blindTimeOut = (registrationTimeout <= 0) ? 99999 : registrationTimeout;
                player.addPotionEffect(bukkitService.createBlindnessEffect(blindTimeOut));
            }
            commandManager.runCommandsOnJoin(player);
            if (pendingRegistration != null) {
                bukkitService.runTaskOptionallyAsync(() -> processPendingRegistration(player, pendingRegistration));
                return;
            }
            if (pendingLoginPassword != null) {
                bukkitService.runTaskOptionallyAsync(() -> asynchronousLogin.login(player, pendingLoginPassword));
                return;
            }
            if (pendingRecoveryEmail != null) {
                bukkitService.runTaskOptionallyAsync(() -> processPendingEmailRecovery(player, pendingRecoveryEmail));
                return;
            }
            if (pendingForceLogin) {
                bukkitService.runTaskOptionallyAsync(() -> asynchronousLogin.forceLogin(player));
                return;
            }
            if (!shouldSkipPostJoinDialog
                && !playerCache.isAuthenticated(player.getName())
                && service.getProperty(RegistrationSettings.USE_DIALOG_UI)
                && dialogAdapter.isDialogSupported()) {
                bukkitService.runTaskLater(player, () -> showPostJoinDialogIfNecessary(player, isAuthAvailable), 1L);
            }
        });
    }

    private void showPostJoinDialogIfNecessary(Player player, boolean isAuthAvailable) {
        if (!player.isOnline()
            || playerCache.isAuthenticated(player.getName())
            || !service.getProperty(RegistrationSettings.USE_DIALOG_UI)
            || !dialogAdapter.isDialogSupported()) {
            return;
        }

        if (isAuthAvailable) {
            dialogAdapter.showLoginDialog(player, dialogWindowService.createLoginDialog(player));
        } else {
            RegistrationType registrationType = service.getProperty(RegistrationSettings.REGISTRATION_TYPE);
            RegisterSecondaryArgument secondArg = service.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT);
            dialogAdapter.showRegisterDialog(player,
                registrationType,
                secondArg,
                dialogWindowService.createRegisterDialog(player, registrationType, secondArg));
        }
        dialogStateService.markDialogOpen(player);
    }

    private void processPendingEmailRecovery(Player player, String submittedEmail) {
        String playerName = player.getName();
        if (!emailService.hasAllInformation()) {
            kickFromPreJoinRecovery(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
            return;
        }
        if (!validationService.validateEmail(submittedEmail)) {
            kickFromPreJoinRecovery(player, MessageKey.INVALID_EMAIL);
            return;
        }
        ch.jalu.datasourcecolumns.data.DataSourceValue<String> emailResult =
            database.getEmail(playerName.toLowerCase(Locale.ROOT));
        if (!emailResult.rowExists()) {
            kickFromPreJoinRecovery(player, MessageKey.USAGE_REGISTER);
            return;
        }
        String registeredEmail = emailResult.getValue();
        if (Utils.isEmailEmpty(registeredEmail) || !registeredEmail.equalsIgnoreCase(submittedEmail)) {
            kickFromPreJoinRecovery(player, MessageKey.INVALID_EMAIL);
            return;
        }
        if (recoveryCodeService.isRecoveryCodeNeeded()) {
            passwordRecoveryService.createAndSendRecoveryCode(player, registeredEmail);
        } else {
            passwordRecoveryService.generateAndSendNewPassword(player, registeredEmail);
        }
        // Player stays in limbo to use the recovery code or new password
    }

    private void kickFromPreJoinRecovery(Player player, MessageKey messageKey) {
        String kickMessage = service.retrieveSingleMessage(player, messageKey);
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, () -> player.kickPlayer(kickMessage));
    }

    private void processPendingRegistration(Player player, PreJoinDialogService.PendingRegistration pendingRegistration) {
        if (pendingRegistration.isEmailRegistration()) {
            asyncRegister.register(RegistrationMethod.EMAIL_REGISTRATION,
                EmailRegisterParams.of(player, pendingRegistration.primaryValue()));
        } else {
            asyncRegister.register(RegistrationMethod.PASSWORD_REGISTRATION,
                PasswordRegisterParams.of(player, pendingRegistration.primaryValue(), pendingRegistration.secondaryValue()));
        }
    }

    /**
     * Checks whether the maximum number of accounts has been exceeded for the given IP address (according to
     * settings and permissions). If this is the case, the player is kicked.
     *
     * @param player the player to verify
     * @param ip     the ip address of the player
     *
     * @return true if the verification is OK (no infraction), false if player has been kicked
     */
    private boolean validatePlayerCountForIp(Player player, String ip) {
        if (service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP) > 0
            && !service.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && !InternetProtocolUtils.isLoopbackAddress(ip)
            && countOnlinePlayersByIp(ip) > service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP)) {

            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player,
                () -> player.kickPlayer(service.retrieveSingleMessage(player, MessageKey.SAME_IP_ONLINE)));
            return false;
        }
        return true;
    }

    private boolean canBypassWithPremium(Player player) {
        if (!service.getProperty(PremiumSettings.ENABLE_PREMIUM)) {
            return false;
        }
        String name = player.getName();
        PlayerAuth auth = database.getAuth(name.toLowerCase(Locale.ROOT));
        if (auth == null) {
            return false;
        }

        if (!auth.isPremium()) {
            UUID playerId = player.getUniqueId();
            if (playerId.version() == 4) {
                // Proxy path: atomic gate prevents double-finalization with concurrent validate().
                UUID pendingUuid = pendingPremiumCache.removePending(name);
                if (pendingUuid == null) {
                    return false;
                }
                if (playerId.equals(pendingUuid)) {
                    premiumService.finalizePendingPremium(player, pendingUuid);
                    return true;
                }
                bungeeSender.sendPremiumUnset(name);
                service.send(player, MessageKey.PREMIUM_PENDING_FAIL);
                return false;
            } else if (bungeeSender.isEnabled()) {
                // Behind a proxy we intentionally keep the backend UUID on the offline v3 value.
                // Wait for the signed perform.login request carrying the verified Mojang UUID
                // instead of failing the pending premium enrollment during join.
                return false;
            } else {
                // No proxy: require cryptographic session verification via PacketEvents.
                UUID pendingUuid = pendingPremiumCache.getPendingUuid(name);
                if (pendingUuid == null) {
                    return false;
                }
                UUID verified = premiumLoginVerifier.getVerifiedUuid(name);
                UUID confirmedUuid = (verified != null && verified.equals(pendingUuid)) ? verified : null;
                pendingPremiumCache.removePending(name);
                if (confirmedUuid != null) {
                    premiumService.finalizePendingPremium(player, confirmedUuid);
                    return true;
                }
                bungeeSender.sendPremiumUnset(name);
                service.send(player, MessageKey.PREMIUM_PENDING_FAIL);
                return false;
            }
        }

        UUID playerId = player.getUniqueId();
        if (playerId.version() == 4) {
            // UUID v4 = Mojang online UUID (online-mode server or proxy forwarding): compare directly.
            // Security relies on the backend port being firewalled to only accept proxy connections.
            return playerId.equals(auth.getPremiumUuid());
        } else if (bungeeSender.isEnabled()) {
            // Behind a proxy, existing premium auto-login is validated through the signed
            // perform.login message rather than the backend player's offline UUID.
            return false;
        }
        // UUID v3 = Bukkit offline UUID: require cryptographic session verification via PacketEvents.
        UUID verifiedUuid = premiumLoginVerifier.getVerifiedUuid(name);
        return verifiedUuid != null && verifiedUuid.equals(auth.getPremiumUuid());
    }

    private int countOnlinePlayersByIp(String ip) {
        int count = 0;
        for (Player player : bukkitService.getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(PlayerUtils.getPlayerIp(player))) {
                ++count;
            }
        }
        return count;
    }
}
