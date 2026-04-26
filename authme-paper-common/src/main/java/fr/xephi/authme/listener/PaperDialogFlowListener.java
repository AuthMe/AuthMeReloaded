package fr.xephi.authme.listener;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.platform.PaperDialogActionKeys;
import fr.xephi.authme.platform.PaperDialogHelper;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.DialogWindowService;
import fr.xephi.authme.service.PreJoinDialogService;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import org.bukkit.Bukkit;

import javax.inject.Inject;
import java.net.InetSocketAddress;
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

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    private final ConcurrentMap<UUID, CompletableFuture<String>> pendingLoginResponses = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CompletableFuture<Boolean>> pendingRegisterResponses = new ConcurrentHashMap<>();

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
    private DialogWindowService dialogWindowService;

    @Inject
    private SessionService sessionService;

    @Inject
    private ProxySessionManager proxySessionManager;

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

        pendingLoginResponses.remove(playerId);
        pendingRegisterResponses.remove(playerId);
        preJoinDialogService.clear(playerId);

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
            handleBlockingLoginDialog(connection, playerId, playerName);
        } else if (commonService.getProperty(RegistrationSettings.FORCE)) {
            RegistrationType registrationType = commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE);
            RegisterSecondaryArgument secondArg =
                commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT);
            handleBlockingRegisterDialog(connection, playerId, PaperDialogHelper.createPreJoinRegisterDialog(
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
            completeLoginResponse(playerId, messages.retrieveSingle(playerName, MessageKey.LOGIN_TIMEOUT_ERROR));
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_LOGIN_SUBMIT.equals(event.getIdentifier())) {
            processPreJoinLogin(playerId, playerName, event.getDialogResponseView());
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_REGISTER_SUBMIT.equals(event.getIdentifier())) {
            storePendingRegistration(playerId, event.getDialogResponseView());
            return;
        }

        if (PaperDialogActionKeys.PRE_JOIN_REGISTER_CANCEL.equals(event.getIdentifier())) {
            completeRegisterResponse(playerId, false);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerConnectionClose(PlayerConnectionCloseEvent event) {
        UUID playerId = event.getPlayerUniqueId();
        pendingLoginResponses.remove(playerId);
        pendingRegisterResponses.remove(playerId);
        preJoinDialogService.clear(playerId);
    }

    private void handleBlockingLoginDialog(PlayerConfigurationConnection connection, UUID playerId, String playerName) {
        CompletableFuture<String> loginResponse = new CompletableFuture<>();
        long timeoutSeconds = Math.max(commonService.getProperty(RestrictionSettings.LOGIN_TIMEOUT), 1);
        loginResponse.completeOnTimeout(
            messages.retrieveSingle(playerName, MessageKey.LOGIN_TIMEOUT_ERROR), timeoutSeconds, TimeUnit.SECONDS);
        pendingLoginResponses.put(playerId, loginResponse);

        connection.getAudience().showDialog(
            PaperDialogHelper.createPreJoinLoginDialog(dialogWindowService.createPreJoinLoginDialog(playerName)));
        String kickMessage = loginResponse.join();
        pendingLoginResponses.remove(playerId);
        connection.getAudience().closeDialog();

        if (kickMessage != null) {
            connection.disconnect(LEGACY_SERIALIZER.deserialize(kickMessage));
        }
    }

    private void processPreJoinLogin(UUID playerId, String playerName, DialogResponseView dialogResponseView) {
        String password = dialogResponseView == null ? null : dialogResponseView.getText("password");
        if (password == null || password.isBlank()) {
            completeLoginResponse(playerId, messages.retrieveSingle(playerName, MessageKey.LOGIN_TIMEOUT_ERROR));
            return;
        }

        PlayerAuth auth = dataSource.getAuth(playerName.toLowerCase(Locale.ROOT));
        if (auth == null) {
            completeLoginResponse(playerId, messages.retrieveSingle(playerName, MessageKey.UNKNOWN_USER));
            return;
        }

        if (passwordSecurity.comparePassword(password, auth.getPassword(), playerName)) {
            preJoinDialogService.storePendingLoginPassword(playerId, password);
            completeLoginResponse(playerId, null);
        } else {
            completeLoginResponse(playerId, messages.retrieveSingle(playerName, MessageKey.WRONG_PASSWORD));
        }
    }

    private void handleBlockingRegisterDialog(PlayerConfigurationConnection connection, UUID playerId, Dialog dialog) {
        CompletableFuture<Boolean> registerResponse = new CompletableFuture<>();
        long timeoutSeconds = Math.max(commonService.getProperty(RestrictionSettings.REGISTER_TIMEOUT), 1);
        registerResponse.completeOnTimeout(true, timeoutSeconds, TimeUnit.SECONDS);
        pendingRegisterResponses.put(playerId, registerResponse);

        connection.getAudience().showDialog(dialog);
        boolean skipPostJoinDialog = registerResponse.join();
        pendingRegisterResponses.remove(playerId);
        connection.getAudience().closeDialog();

        if (skipPostJoinDialog) {
            preJoinDialogService.markSkipPostJoinDialog(playerId);
        }
    }

    private void storePendingRegistration(UUID playerId, DialogResponseView dialogResponseView) {
        if (dialogResponseView == null) {
            completeRegisterResponse(playerId, false);
            return;
        }

        RegistrationType registrationType = commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE);
        RegisterSecondaryArgument secondArg = commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT);
        if (registrationType == RegistrationType.EMAIL) {
            String email = dialogResponseView.getText("email");
            String confirm = dialogResponseView.getText("confirm");
            if (email == null || !validationService.validateEmail(email)) {
                completeRegisterResponse(playerId, false);
                return;
            }
            if (secondArg == RegisterSecondaryArgument.CONFIRMATION && !email.equals(confirm)) {
                completeRegisterResponse(playerId, false);
                return;
            }
            preJoinDialogService.storePendingEmailRegistration(playerId, email);
            completeRegisterResponse(playerId, false);
            return;
        }

        String password = dialogResponseView.getText("password");
        if (password == null || password.isBlank()) {
            completeRegisterResponse(playerId, false);
            return;
        }
        if (secondArg == RegisterSecondaryArgument.CONFIRMATION) {
            String confirm = dialogResponseView.getText("confirm");
            if (!password.equals(confirm)) {
                completeRegisterResponse(playerId, false);
                return;
            }
            preJoinDialogService.storePendingPasswordRegistration(playerId, password, null);
            completeRegisterResponse(playerId, false);
            return;
        }

        if (secondArg == RegisterSecondaryArgument.EMAIL_MANDATORY
            || secondArg == RegisterSecondaryArgument.EMAIL_OPTIONAL) {
            String email = dialogResponseView.getText("email");
            if (secondArg == RegisterSecondaryArgument.EMAIL_MANDATORY && (email == null || email.isBlank())) {
                completeRegisterResponse(playerId, false);
                return;
            }
            if (email != null && !email.isBlank() && !validationService.validateEmail(email)) {
                completeRegisterResponse(playerId, false);
                return;
            }
            preJoinDialogService.storePendingPasswordRegistration(playerId, password, email);
            completeRegisterResponse(playerId, false);
            return;
        }

        preJoinDialogService.storePendingPasswordRegistration(playerId, password, null);
        completeRegisterResponse(playerId, false);
    }

    private void completeLoginResponse(UUID playerId, String kickMessage) {
        CompletableFuture<String> loginResponse = pendingLoginResponses.get(playerId);
        if (loginResponse != null) {
            loginResponse.complete(kickMessage);
        }
    }

    private void completeRegisterResponse(UUID playerId, boolean skipPostJoinDialog) {
        CompletableFuture<Boolean> registerResponse = pendingRegisterResponses.get(playerId);
        if (registerResponse != null) {
            registerResponse.complete(skipPostJoinDialog);
        }
    }

    // MC 1.21.6 (protocol 771) introduced the dialog / custom-click packets required for pre-join dialogs
    private static final int DIALOG_MIN_PROTOCOL = 771;

    private boolean shouldSkipDialogs(String normalizedName, PlayerConfigurationConnection connection) {
        if (playerCache.isAuthenticated(normalizedName) || proxySessionManager.shouldResumeSession(normalizedName)) {
            return true;
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
}
