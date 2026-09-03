package fr.xephi.authme.listener;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.platform.DialogWindowSpec;
import fr.xephi.authme.platform.PaperDialogActionKeys;
import fr.xephi.authme.platform.PaperDialogHelper;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.DialogWindowService;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PreJoinDialogService;
import fr.xephi.authme.service.PremiumLoginVerifier;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.properties.PremiumSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.InternetProtocolUtils;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.Bukkit;

import javax.inject.Inject;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentMap;

/**
 * Handles Paper/Folia dialog flows that happen during the configuration phase.
 */
public class PaperDialogFlowListener implements Listener {

    // Map session id (returned by PreJoinDialogService.registerPreJoinFuture) -> future
    private final ConcurrentMap<Long, CompletableFuture<String>> pendingLoginResponses = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, CompletableFuture<String>> pendingRegisterResponses = new ConcurrentHashMap<>();
    // Map connection object -> session id so event handlers can resolve the correct session
    // Note: multiple concurrent connections for the same player name/UUID may exist (e.g. proxy), so we must
    // track sessions by connection object
    private final ConcurrentMap<PlayerConfigurationConnection, Long> connectionSessions = new ConcurrentHashMap<>();

    @Inject
    private CommonService commonService;

    @Inject
    private DataSource dataSource;

    @Inject
    private Messages messages;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private ValidationService validationService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private PreJoinDialogService preJoinDialogService;

    @Inject
    private PendingPremiumCache pendingPremiumCache;

    @Inject
    private DialogWindowService dialogWindowService;

    @Inject
    private SessionService sessionService;

    @Inject
    private ProxySessionManager proxySessionManager;

    @Inject
    private PremiumLoginVerifier premiumLoginVerifier;

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerConfigure(AsyncPlayerConnectionConfigureEvent event) {
        if (!commonService.getProperty(RegistrationSettings.USE_PREJOIN_DIALOG_UI)) {
            return;
        }

        PlayerConfigurationConnection connection = event.getConnection();
        PlayerProfile profile = connection.getProfile();
        UUID playerId = profile.getId();
        String playerName = profile.getName();
        if (playerId == null || playerName == null) {
            return;
        }

        // Clear any previous session for this specific connection (if any). Don'internal clear by UUID;
        // that would affect other concurrent connections for the same player name/UUID
        // but from different clients/connections.
        cleanup(connection);

        String normalizedName = playerName.toLowerCase(Locale.ROOT);
        Set<String> unrestrictedNames = commonService.getProperty(RestrictionSettings.UNRESTRICTED_NAMES);
        if (unrestrictedNames.contains(normalizedName)) {
            return;
        }
        if (shouldSkipDialogs(normalizedName, connection)) {
            return;
        }

        PlayerAuth auth = dataSource.getAuth(normalizedName);
        if (auth != null) {
            if (shouldSkipPreJoinDialogForPremium(auth, playerName, playerId)) {
                preJoinDialogService.markSkipPostJoinDialog(playerId);
                return;
            }
            handleBlockingLoginDialog(connection, playerId, playerName);
        } else if (commonService.getProperty(RegistrationSettings.FORCE)) {
            RegistrationType registrationType = commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE);
            RegisterSecondaryArgument secondArg =
                commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT);
            handleBlockingRegisterDialog(connection, playerId, playerName, PaperDialogHelper.createPreJoinRegisterDialog(
                dialogWindowService.createPreJoinRegisterDialog(playerName, registrationType, secondArg)));
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerCustomClick(PlayerCustomClickEvent event) {
        if (!(event.getCommonConnection() instanceof PlayerConfigurationConnection connection)) {
            return;
        }

        UUID playerId = connection.getProfile().getId();
        String playerName = connection.getProfile().getName();
        if (playerId == null || playerName == null) {
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_LOGIN_CANCEL.equals(event.getIdentifier())) {
            String kickMessage = commonService.getProperty(RegistrationSettings.PRE_JOIN_LOGIN_CANCEL_KICKS)
                ? messages.retrieveSingle(playerName, MessageKey.DIALOG_LOGIN_CANCELED)
                : null;
            completeLoginResponseForSession(connectionSessions.get(connection), kickMessage);
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_LOGIN_RECOVERY.equals(event.getIdentifier())) {
            processPreJoinLoginRecovery(playerId, playerName, connection);
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_RECOVERY_SUBMIT.equals(event.getIdentifier())) {
            processPreJoinRecoverySubmit(connection, connectionSessions.get(connection), playerId, playerName, event.getDialogResponseView());
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_RECOVERY_CANCEL.equals(event.getIdentifier())) {
            String kickMessage = commonService.getProperty(RegistrationSettings.PRE_JOIN_LOGIN_CANCEL_KICKS)
                ? messages.retrieveSingle(playerName, MessageKey.DIALOG_LOGIN_CANCELED)
                : null;
            completeLoginResponseForSession(connectionSessions.get(connection), kickMessage);
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_LOGIN_SUBMIT.equals(event.getIdentifier())) {
            processPreJoinLoginForSession(connection, connectionSessions.get(connection), playerId, playerName, event.getDialogResponseView());
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_REGISTER_SUBMIT.equals(event.getIdentifier())) {
            storePendingRegistration(connection, playerId, playerName, event.getDialogResponseView());
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_REGISTER_CANCEL.equals(event.getIdentifier())) {
            String kickMessage = commonService.getProperty(RegistrationSettings.PRE_JOIN_REGISTER_CANCEL_KICKS)
                ? messages.retrieveSingle(playerName, MessageKey.DIALOG_REGISTER_CANCELED)
                : null;
            completeRegisterResponseForSession(connectionSessions.get(connection), kickMessage);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerConnectionClose(PlayerConnectionCloseEvent event) {
        UUID playerId = event.getPlayerUniqueId();

        // remove all disconnected sessions
        // pending map is likely very small, but we are still doing a heavy iteration here
        connectionSessions.entrySet().removeIf(entry -> {
            if (entry.getKey().isConnected()) {
                return false;
            }

            Long sid = entry.getValue();
            pendingLoginResponses.remove(sid);
            pendingRegisterResponses.remove(sid);
            preJoinDialogService.unregisterPreJoinFuture(sid);
            return true;
        });
    }

    private void handleBlockingLoginDialog(PlayerConfigurationConnection connection, UUID playerId, String playerName) {
        CompletableFuture<String> loginResponse = new CompletableFuture<>();
        long timeoutSeconds = Math.max(commonService.getProperty(RestrictionSettings.LOGIN_TIMEOUT), 1);
        loginResponse.completeOnTimeout(
            messages.retrieveSingle(playerName, MessageKey.LOGIN_TIMEOUT_ERROR), timeoutSeconds, TimeUnit.SECONDS);
        String normalizedName = playerName.toLowerCase(Locale.ROOT);
        long sid = preJoinDialogService.registerPreJoinFuture(normalizedName, playerId, loginResponse);
        pendingLoginResponses.put(sid, loginResponse);
        connectionSessions.put(connection, sid);

        // Close the race with a proxy auto-login (perform.login) that arrived during the configuration
        // phase between the shouldSkipDialogs() check and now: if a proxy session has been queued,
        // force-login instead of showing the dialog.
        if (proxySessionManager.shouldResumeSession(normalizedName)) {
            ProxySessionManager.ProxyLoginRequest req = proxySessionManager.getLoginRequest(normalizedName);
            if (req != null && (req.verifiedPremiumUuid() == null
                || isProxyPremiumRequestValid(normalizedName, req))) {
                preJoinDialogService.approvePreJoinForceLoginForSession(sid);
            }
        }

        if (!loginResponse.isDone()) {
            connection.getAudience().showDialog(
                PaperDialogHelper.createPreJoinLoginDialog(dialogWindowService.createPreJoinLoginDialog(playerName)));
        }
        String kickMessage = loginResponse.join();
        pendingLoginResponses.remove(sid);
        connectionSessions.remove(connection, sid);
        preJoinDialogService.unregisterPreJoinFuture(sid);

        if (kickMessage != null) {
            preJoinDialogService.storePendingKickMessage(playerId, kickMessage);
        }
        connection.getAudience().closeDialog();
    }

    private void processPreJoinLoginForSession(PlayerConfigurationConnection commonConnection, Long sid, UUID playerId, String playerName, DialogResponseView dialogResponseView) {
        String password = dialogResponseView == null ? null : dialogResponseView.getText("password");
        if (password == null || password.isBlank()) {
            completeLoginResponseForSession(sid, messages.retrieveSingle(playerName, MessageKey.LOGIN_TIMEOUT_ERROR));
            return;
        }

        PlayerAuth auth = dataSource.getAuth(playerName.toLowerCase(Locale.ROOT));
        if (auth == null) {
            completeLoginResponseForSession(sid, messages.retrieveSingle(playerName, MessageKey.UNKNOWN_USER));
            return;
        }

        if (passwordSecurity.comparePassword(password, auth.getPassword(), playerName)) {
            preJoinDialogService.storePendingLoginPassword(commonConnection.getClientAddress(), playerId, password);
            completeLoginResponseForSession(sid, null);
        } else {
            completeLoginResponseForSession(sid, messages.retrieveSingle(playerName, MessageKey.WRONG_PASSWORD));
        }
    }

    private void processPreJoinLoginRecovery(UUID playerId, String playerName,
                                             PlayerConfigurationConnection connection) {
        DialogWindowSpec recoverySpec = dialogWindowService.createPreJoinRecoveryDialog(playerName);
        connection.getAudience().showDialog(PaperDialogHelper.createPreJoinRecoveryDialog(recoverySpec));
    }

    private void processPreJoinRecoverySubmit(PlayerConfigurationConnection connection, Long sid, UUID playerId, String playerName, DialogResponseView dialogResponseView) {
        String email = dialogResponseView == null ? null : dialogResponseView.getText("email");
        if (email != null && !email.isBlank()) {
            preJoinDialogService.storePendingRecoveryEmail(connection.getClientAddress(), playerId, email);
        }

        // Let the player join; AsynchronousJoin will execute the recovery and kick on failure
        completeLoginResponseForSession(sid, null);
    }

    private void handleBlockingRegisterDialog(PlayerConfigurationConnection connection, UUID playerId, String playerName, Dialog dialog) {
        CompletableFuture<String> registerResponse = new CompletableFuture<>();
        long timeoutSeconds = Math.max(commonService.getProperty(RestrictionSettings.REGISTER_TIMEOUT), 1);
        registerResponse.completeOnTimeout(
            messages.retrieveSingle(playerName, MessageKey.LOGIN_TIMEOUT_ERROR), timeoutSeconds, TimeUnit.SECONDS);
        long sid = preJoinDialogService.registerPreJoinFuture(playerName.toLowerCase(Locale.ROOT), playerId, registerResponse);
        pendingRegisterResponses.put(sid, registerResponse);
        connectionSessions.put(connection, sid);

        connection.getAudience().showDialog(dialog);
        String kickMessage = registerResponse.join();
        pendingRegisterResponses.remove(sid);
        connectionSessions.remove(connection, sid);
        preJoinDialogService.unregisterPreJoinFuture(sid);

        if (kickMessage != null) {
            preJoinDialogService.storePendingKickMessage(playerId, kickMessage);
        }
        connection.getAudience().closeDialog();
    }

    private void storePendingRegistration(PlayerConfigurationConnection connection, UUID playerId, String playerName,
                                          DialogResponseView dialogResponseView) {
        if (dialogResponseView == null) {
            completeRegisterResponseForSession(connectionSessions.get(connection), null);
            return;
        }

        InetSocketAddress clientAddress = connection.getClientAddress();
        if (clientAddress != null) {
            String ip = clientAddress.getAddress().getHostAddress();
            int maxRegPerIp = commonService.getProperty(RestrictionSettings.MAX_REGISTRATION_PER_IP);
            if (maxRegPerIp > 0 && !InternetProtocolUtils.isLoopbackAddress(ip)) {
                List<String> otherAccounts = dataSource.getAllAuthsByIp(ip);
                if (otherAccounts.size() >= maxRegPerIp) {
                    String kickMessage = messages.retrieveSingle(playerName, MessageKey.MAX_REGISTER_EXCEEDED,
                        Integer.toString(maxRegPerIp), Integer.toString(otherAccounts.size()),
                        String.join(", ", otherAccounts));
                    completeRegisterResponseForSession(connectionSessions.get(connection), kickMessage);
                    return;
                }
            }
        }

        RegistrationType registrationType = commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE);
        RegisterSecondaryArgument secondArg = commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT);
        if (registrationType == RegistrationType.EMAIL) {
            String email = dialogResponseView.getText("email");
            String confirm = dialogResponseView.getText("confirm");
            if (email == null || email.isBlank() || !validationService.validateEmail(email)) {
                showRegisterDialogWithError(connection, playerName,
                    messages.retrieveSingle(playerName, MessageKey.INVALID_EMAIL));
                return;
            }
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION && !email.equals(confirm)) {
                showRegisterDialogWithError(connection, playerName,
                    messages.retrieveSingle(playerName, MessageKey.PASSWORD_MATCH_ERROR));
                return;
            }
            preJoinDialogService.storePendingEmailRegistration(connection.getClientAddress(), playerId, email);
            completeRegisterResponseForSession(connectionSessions.get(connection), null);
            return;
        }

        String password = dialogResponseView.getText("password");
        if (password == null || password.isBlank()) {
            showRegisterDialogWithError(connection, playerName,
                messages.retrieveSingle(playerName, MessageKey.INVALID_PASSWORD_LENGTH));
            return;
        }
        if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
            String confirm = dialogResponseView.getText("confirm");
            if (!password.equals(confirm)) {
                showRegisterDialogWithError(connection, playerName,
                    messages.retrieveSingle(playerName, MessageKey.PASSWORD_MATCH_ERROR));
                return;
            }
        }
        ValidationService.ValidationResult passwordResult = validationService.validatePassword(password, playerName);
        if (passwordResult.hasError()) {
            showRegisterDialogWithError(connection, playerName,
                messages.retrieveSingle(playerName, passwordResult.getMessageKey(), passwordResult.getArgs()));
            return;
        }
        if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
            preJoinDialogService.storePendingPasswordRegistration(connection.getClientAddress(), playerId, password, null);
            completeRegisterResponseForSession(connectionSessions.get(connection), null);
            return;
        }

        if (secondArg == RegisterSecondaryArgument.EMAIL_MANDATORY
            || secondArg == RegisterSecondaryArgument.EMAIL_OPTIONAL) {
            String email = dialogResponseView.getText("email");
            if (secondArg == RegisterSecondaryArgument.EMAIL_MANDATORY && (email == null || email.isBlank())) {
                showRegisterDialogWithError(connection, playerName,
                    messages.retrieveSingle(playerName, MessageKey.INVALID_EMAIL));
                return;
            }
            if (email != null && !email.isBlank() && !validationService.validateEmail(email)) {
                showRegisterDialogWithError(connection, playerName,
                    messages.retrieveSingle(playerName, MessageKey.INVALID_EMAIL));
                return;
            }
            completeRegisterResponseForSession(connectionSessions.get(connection), null);
            return;
        }

        completeRegisterResponseForSession(connectionSessions.get(connection), null);
    }

    private void showRegisterDialogWithError(PlayerConfigurationConnection connection, String playerName,
                                             String errorMessage) {
        RegistrationType registrationType = commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE);
        RegisterSecondaryArgument secondArg = commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT);
        DialogWindowSpec spec = dialogWindowService.createPreJoinRegisterDialog(playerName, registrationType, secondArg);
        connection.getAudience().showDialog(PaperDialogHelper.createPreJoinRegisterDialog(spec.withBody(errorMessage)));
    }

    private void completeLoginResponseForSession(Long sessionId, String kickMessage) {
        if (sessionId == null) {
            return;
        }

        CompletableFuture<String> loginResponse = pendingLoginResponses.get(sessionId);
        if (loginResponse != null) {
            loginResponse.complete(kickMessage);
        }
    }

    private void completeRegisterResponseForSession(Long sessionId, String kickMessage) {
        if (sessionId == null) {
            return;
        }

        CompletableFuture<String> registerResponse = pendingRegisterResponses.get(sessionId);
        if (registerResponse != null) {
            registerResponse.complete(kickMessage);
        }
    }

    private boolean shouldSkipPreJoinDialogForPremium(PlayerAuth auth, String playerName, UUID playerId) {
        if (!commonService.getProperty(PremiumSettings.ENABLE_PREMIUM) || !auth.isPremium()) {
            return false;
        }
        if (playerId != null && playerId.version() == 4) {
            // UUID v4: Mojang UUID already in the profile (online-mode or proxy forwarded it).
            return playerId.equals(auth.getPremiumUuid());
        }
        // UUID v3 (offline): check if PacketEvents has already completed verification.
        UUID verifiedUuid = premiumLoginVerifier.getVerifiedUuid(playerName);
        if (verifiedUuid != null) {
            return verifiedUuid.equals(auth.getPremiumUuid());
        }
        // UUID is still offline at the configure phase but the player is enrolled as premium.
        // Proxy UUID forwarding may not have been applied yet — skip the blocking pre-join dialog
        // and let AsynchronousJoin.canBypassWithPremium() do the definitive check once the player
        // has fully joined and player.getUniqueId() reflects the proxy-forwarded Mojang UUID.
        return true;
    }

    private boolean isProxyPremiumRequestValid(String normalizedName, ProxySessionManager.ProxyLoginRequest request) {
        UUID verifiedUuid = request.verifiedPremiumUuid();
        if (verifiedUuid == null) {
            return true;
        }
        PlayerAuth auth = dataSource.getAuth(normalizedName);
        if (auth == null) {
            return false;
        }
        if (auth.isPremium()) {
            return verifiedUuid.equals(auth.getPremiumUuid());
        }
        UUID pendingUuid = pendingPremiumCache.getPendingUuid(normalizedName);
        return pendingUuid != null && verifiedUuid.equals(pendingUuid);
    }

    // MC 1.21.6 (protocol 771) introduced the dialog / custom-click packets required for pre-join dialogs
    private static final int DIALOG_MIN_PROTOCOL = 771;

    private boolean shouldSkipDialogs(String normalizedName, PlayerConfigurationConnection connection) {
        if (playerCache.isAuthenticated(normalizedName)) {
            return true;
        }

        if (proxySessionManager.shouldResumeSession(normalizedName)) {
            ProxySessionManager.ProxyLoginRequest request = proxySessionManager.getLoginRequest(normalizedName);
            if (request != null && (request.verifiedPremiumUuid() == null
                || isProxyPremiumRequestValid(normalizedName, request))) {
                return true;
            }
        }

        InetSocketAddress clientAddress = connection.getClientAddress();
        String ipAddress = clientAddress == null ? null : clientAddress.getAddress().getHostAddress();
        if (sessionService.hasValidSession(normalizedName, ipAddress)) {
            return true;
        }

        return !isClientDialogCapable(connection.getProfile().getId());
    }

    private static boolean isClientDialogCapable(UUID playerId) {
        try {
            if (Bukkit.getPluginManager().getPlugin("ViaVersion") == null) {
                return true;
            }
            Class<?> viaApiClass = Class.forName("com.viaversion.viaversion.api.ViaAPI");
            Class<?> viaClass = Class.forName("com.viaversion.viaversion.api.Via");
            Object api = viaClass.getMethod("getAPI").invoke(null);
            int version = (int) viaApiClass.getMethod("getPlayerVersion", UUID.class).invoke(api, playerId);
            return version >= DIALOG_MIN_PROTOCOL;
        } catch (Exception ignored) {
            return true;
        }
    }

    private void cleanup(PlayerConfigurationConnection connection) {
        Long sid = connectionSessions.remove(connection);
        if (sid != null) {
            pendingLoginResponses.remove(sid);
            pendingRegisterResponses.remove(sid);
            preJoinDialogService.unregisterPreJoinFuture(sid);
        }
    }
}
