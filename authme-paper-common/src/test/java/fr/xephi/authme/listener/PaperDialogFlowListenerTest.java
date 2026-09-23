package fr.xephi.authme.listener;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.platform.DialogInputSpec;
import fr.xephi.authme.platform.DialogWindowSpec;
import fr.xephi.authme.platform.PaperDialogActionKeys;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.DialogWindowService;
import fr.xephi.authme.service.PreJoinDialogService;
import fr.xephi.authme.service.PremiumLoginVerifier;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.properties.PremiumSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import fr.xephi.authme.platform.PaperDialogHelper;
import net.kyori.adventure.audience.Audience;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class PaperDialogFlowListenerTest {

    private static final long SESSION_ID = 42L;

    @Test
    public void shouldListenLateOnPlayerConfigure() throws Exception {
        Method method = PaperDialogFlowListener.class
            .getDeclaredMethod("onPlayerConfigure", AsyncPlayerConnectionConfigureEvent.class);

        EventHandler annotation = method.getAnnotation(EventHandler.class);

        assertThat(annotation.priority(), is(EventPriority.HIGHEST));
    }

    @Test
    public void shouldShowErrorDialogAndKeepFutureOpenForEmptyPasswordSubmission() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        Messages messages = mock(Messages.class);
        ValidationService validationService = mock(ValidationService.class);
        DialogWindowService dialogWindowService = mock(DialogWindowService.class);
        PreJoinDialogService preJoinDialogService = mock(PreJoinDialogService.class);
        setField(listener, "commonService", commonService);
        setField(listener, "messages", messages);
        setField(listener, "validationService", validationService);
        setField(listener, "dialogWindowService", dialogWindowService);
        setField(listener, "preJoinDialogService", preJoinDialogService);

        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.PASSWORD);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT))
            .willReturn(RegisterSecondaryArgument.CONFIRMATION);
        given(messages.retrieveSingle("Bobby", MessageKey.INVALID_PASSWORD_LENGTH)).willReturn("Password too short!");
        given(dialogWindowService.createPreJoinRegisterDialog("Bobby", RegistrationType.PASSWORD,
            RegisterSecondaryArgument.CONFIRMATION)).willReturn(
            new DialogWindowSpec("Register", List.of(new DialogInputSpec("password", "Password", 100)),
                "Register", "Cancel", false, false, null));

        CompletableFuture<String> future = new CompletableFuture<>();
        PlayerConfigurationConnection connection = mockConnection("Bobby");
        given(connection.getAudience()).willReturn(mock(Audience.class));
        seedSession(listener, connection, "pendingRegisterResponses", future);

        DialogResponseView responseView = mock(DialogResponseView.class);
        given(responseView.getText("password")).willReturn("");

        PlayerCustomClickEvent event = mock(PlayerCustomClickEvent.class);
        given(event.getCommonConnection()).willReturn(connection);
        given(event.getIdentifier()).willReturn(PaperDialogActionKeys.PRE_JOIN_REGISTER_SUBMIT);
        given(event.getDialogResponseView()).willReturn(responseView);

        // PaperDialogHelper.createPreJoinRegisterDialog() requires Paper registry internals;
        // mock the static to avoid that dependency and focus on behavioral assertions.
        try (var helperStatic = mockStatic(PaperDialogHelper.class)) {
            helperStatic.when(() -> PaperDialogHelper.createPreJoinRegisterDialog(any())).thenReturn(null);

            listener.onPlayerCustomClick(event);

            assertThat("future must stay open so the player can retry", future.isDone(), is(false));
            verify(connection).getAudience();
            verify(preJoinDialogService, never()).storePendingPasswordRegistration(anyLong(), any(), any());
        }
    }

    @Test
    public void shouldFallbackToPostJoinDialogWhenPreJoinLoginIsCancelled() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        setField(listener, "commonService", commonService);
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_LOGIN_CANCEL_KICKS)).willReturn(false);

        CompletableFuture<String> future = new CompletableFuture<>();
        PlayerConfigurationConnection connection = mockConnection("Bobby");
        seedSession(listener, connection, "pendingLoginResponses", future);

        PlayerCustomClickEvent event = mock(PlayerCustomClickEvent.class);
        given(event.getCommonConnection()).willReturn(connection);
        given(event.getIdentifier()).willReturn(PaperDialogActionKeys.PRE_JOIN_LOGIN_CANCEL);

        listener.onPlayerCustomClick(event);

        assertThat(future.isDone(), is(true));
        assertThat(future.getNow("sentinel"), is((String) null));
    }

    @Test
    public void shouldKickWhenPreJoinLoginIsCancelledAndSettingEnabled() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        Messages messages = mock(Messages.class);
        setField(listener, "commonService", commonService);
        setField(listener, "messages", messages);
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_LOGIN_CANCEL_KICKS)).willReturn(true);
        given(messages.retrieveSingle("Bobby", MessageKey.DIALOG_LOGIN_CANCELED)).willReturn("Canceled!");

        CompletableFuture<String> future = new CompletableFuture<>();
        PlayerConfigurationConnection connection = mockConnection("Bobby");
        seedSession(listener, connection, "pendingLoginResponses", future);

        PlayerCustomClickEvent event = mock(PlayerCustomClickEvent.class);
        given(event.getCommonConnection()).willReturn(connection);
        given(event.getIdentifier()).willReturn(PaperDialogActionKeys.PRE_JOIN_LOGIN_CANCEL);

        listener.onPlayerCustomClick(event);

        assertThat(future.isDone(), is(true));
        assertThat(future.getNow(null), is("Canceled!"));
    }

    @Test
    public void shouldFallbackToPostJoinDialogWhenPreJoinRegisterIsCancelled() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        setField(listener, "commonService", commonService);
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_REGISTER_CANCEL_KICKS)).willReturn(false);

        CompletableFuture<String> future = new CompletableFuture<>();
        PlayerConfigurationConnection connection = mockConnection("Bobby");
        seedSession(listener, connection, "pendingRegisterResponses", future);

        PlayerCustomClickEvent event = mock(PlayerCustomClickEvent.class);
        given(event.getCommonConnection()).willReturn(connection);
        given(event.getIdentifier()).willReturn(PaperDialogActionKeys.PRE_JOIN_REGISTER_CANCEL);

        listener.onPlayerCustomClick(event);

        assertThat(future.isDone(), is(true));
        assertThat(future.getNow("sentinel"), is((String) null));
    }

    @Test
    public void shouldKickWhenPreJoinRegisterIsCancelledAndSettingEnabled() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        Messages messages = mock(Messages.class);
        setField(listener, "commonService", commonService);
        setField(listener, "messages", messages);
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_REGISTER_CANCEL_KICKS)).willReturn(true);
        given(messages.retrieveSingle("Bobby", MessageKey.DIALOG_REGISTER_CANCELED)).willReturn("Canceled!");

        CompletableFuture<String> future = new CompletableFuture<>();
        PlayerConfigurationConnection connection = mockConnection("Bobby");
        seedSession(listener, connection, "pendingRegisterResponses", future);

        PlayerCustomClickEvent event = mock(PlayerCustomClickEvent.class);
        given(event.getCommonConnection()).willReturn(connection);
        given(event.getIdentifier()).willReturn(PaperDialogActionKeys.PRE_JOIN_REGISTER_CANCEL);

        listener.onPlayerCustomClick(event);

        assertThat(future.isDone(), is(true));
        assertThat(future.getNow(null), is("Canceled!"));
    }

    @Test
    public void shouldSkipPreJoinDialogsForAuthenticatedPlayerEvenIfPostJoinDialogsAreDisabled() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        PlayerCache playerCache = mock(PlayerCache.class);
        PreJoinDialogService preJoinDialogService = mock(PreJoinDialogService.class);
        SessionService sessionService = mock(SessionService.class);
        ProxySessionManager proxySessionManager = mock(ProxySessionManager.class);
        setField(listener, "commonService", commonService);
        setField(listener, "playerCache", playerCache);
        setField(listener, "preJoinDialogService", preJoinDialogService);
        setField(listener, "sessionService", sessionService);
        setField(listener, "proxySessionManager", proxySessionManager);

        given(commonService.getProperty(RegistrationSettings.USE_DIALOG_UI)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.USE_PREJOIN_DIALOG_UI)).willReturn(true);
        given(commonService.getProperty(RestrictionSettings.UNRESTRICTED_NAMES)).willReturn(Set.of());
        given(playerCache.isAuthenticated("bobby")).willReturn(true);

        UUID playerId = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getId()).willReturn(playerId);
        given(profile.getName()).willReturn("Bobby");

        Audience audience = mock(Audience.class);
        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        given(connection.getProfile()).willReturn(profile);
        given(connection.getAudience()).willReturn(audience);

        AsyncPlayerConnectionConfigureEvent event = mock(AsyncPlayerConnectionConfigureEvent.class);
        given(event.getConnection()).willReturn(connection);

        listener.onPlayerConfigure(event);

        verify(preJoinDialogService, never()).openSession(anyString());
        verifyNoInteractions(audience);
    }

    @Test
    public void shouldSkipPreJoinDialogsForPlayerWithValidSession() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        PlayerCache playerCache = mock(PlayerCache.class);
        PreJoinDialogService preJoinDialogService = mock(PreJoinDialogService.class);
        SessionService sessionService = mock(SessionService.class);
        ProxySessionManager proxySessionManager = mock(ProxySessionManager.class);
        setField(listener, "commonService", commonService);
        setField(listener, "playerCache", playerCache);
        setField(listener, "preJoinDialogService", preJoinDialogService);
        setField(listener, "sessionService", sessionService);
        setField(listener, "proxySessionManager", proxySessionManager);

        given(commonService.getProperty(RegistrationSettings.USE_PREJOIN_DIALOG_UI)).willReturn(true);
        given(commonService.getProperty(RestrictionSettings.UNRESTRICTED_NAMES)).willReturn(Set.of());
        given(playerCache.isAuthenticated("bobby")).willReturn(false);
        given(proxySessionManager.shouldResumeSession("bobby")).willReturn(false);
        given(sessionService.hasValidSession("bobby", "203.0.113.5")).willReturn(true);

        UUID playerId = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getId()).willReturn(playerId);
        given(profile.getName()).willReturn("Bobby");

        Audience audience = mock(Audience.class);
        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        given(connection.getProfile()).willReturn(profile);
        given(connection.getAudience()).willReturn(audience);
        given(connection.getClientAddress())
            .willReturn(new InetSocketAddress(InetAddress.getByName("203.0.113.5"), 25565));

        AsyncPlayerConnectionConfigureEvent event = mock(AsyncPlayerConnectionConfigureEvent.class);
        given(event.getConnection()).willReturn(connection);

        listener.onPlayerConfigure(event);

        verify(preJoinDialogService, never()).openSession(anyString());
        verifyNoInteractions(audience);
    }

    @Test
    public void shouldSkipPreJoinDialogsForProxyAutoLogin() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        PlayerCache playerCache = mock(PlayerCache.class);
        PreJoinDialogService preJoinDialogService = mock(PreJoinDialogService.class);
        SessionService sessionService = mock(SessionService.class);
        ProxySessionManager proxySessionManager = mock(ProxySessionManager.class);
        setField(listener, "commonService", commonService);
        setField(listener, "playerCache", playerCache);
        setField(listener, "preJoinDialogService", preJoinDialogService);
        setField(listener, "sessionService", sessionService);
        setField(listener, "proxySessionManager", proxySessionManager);

        given(commonService.getProperty(RegistrationSettings.USE_PREJOIN_DIALOG_UI)).willReturn(true);
        given(commonService.getProperty(RestrictionSettings.UNRESTRICTED_NAMES)).willReturn(Set.of());
        given(playerCache.isAuthenticated("bobby")).willReturn(false);
        given(proxySessionManager.shouldResumeSession("bobby")).willReturn(true);
        given(proxySessionManager.getLoginRequest("bobby"))
            .willReturn(new ProxySessionManager.ProxyLoginRequest("bobby", null));

        UUID playerId = UUID.randomUUID();
        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getId()).willReturn(playerId);
        given(profile.getName()).willReturn("Bobby");

        Audience audience = mock(Audience.class);
        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        given(connection.getProfile()).willReturn(profile);
        given(connection.getAudience()).willReturn(audience);

        AsyncPlayerConnectionConfigureEvent event = mock(AsyncPlayerConnectionConfigureEvent.class);
        given(event.getConnection()).willReturn(connection);

        listener.onPlayerConfigure(event);

        verify(preJoinDialogService, never()).openSession(anyString());
        verifyNoInteractions(audience);
        verifyNoInteractions(sessionService);
    }

    @Test
    public void shouldSkipPreJoinDialogsForVerifiedPremiumPlayer() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        PlayerCache playerCache = mock(PlayerCache.class);
        DataSource dataSource = mock(DataSource.class);
        PreJoinDialogService preJoinDialogService = mock(PreJoinDialogService.class);
        SessionService sessionService = mock(SessionService.class);
        ProxySessionManager proxySessionManager = mock(ProxySessionManager.class);
        PremiumLoginVerifier premiumLoginVerifier = mock(PremiumLoginVerifier.class);
        setField(listener, "commonService", commonService);
        setField(listener, "playerCache", playerCache);
        setField(listener, "dataSource", dataSource);
        setField(listener, "preJoinDialogService", preJoinDialogService);
        setField(listener, "sessionService", sessionService);
        setField(listener, "proxySessionManager", proxySessionManager);
        setField(listener, "premiumLoginVerifier", premiumLoginVerifier);

        UUID premiumUuid = UUID.randomUUID();
        // Offline (v3) UUID simulates an offline-mode backend without proxy
        UUID playerId = UUID.nameUUIDFromBytes("bobby".getBytes());
        PlayerAuth auth = PlayerAuth.builder()
            .name("bobby")
            .password(new HashedPassword("hash"))
            .premiumUuid(premiumUuid)
            .build();

        given(commonService.getProperty(RegistrationSettings.USE_PREJOIN_DIALOG_UI)).willReturn(true);
        given(commonService.getProperty(RestrictionSettings.UNRESTRICTED_NAMES)).willReturn(Set.of());
        given(commonService.getProperty(PremiumSettings.ENABLE_PREMIUM)).willReturn(true);
        given(playerCache.isAuthenticated("bobby")).willReturn(false);
        given(proxySessionManager.shouldResumeSession("bobby")).willReturn(false);
        given(sessionService.hasValidSession("bobby", null)).willReturn(false);
        given(dataSource.getAuth("bobby")).willReturn(auth);
        given(premiumLoginVerifier.getVerifiedUuid("Bobby")).willReturn(premiumUuid);
        given(preJoinDialogService.openSession("bobby")).willReturn(SESSION_ID);

        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getId()).willReturn(playerId);
        given(profile.getName()).willReturn("Bobby");

        Audience audience = mock(Audience.class);
        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        given(connection.getProfile()).willReturn(profile);
        given(connection.getAudience()).willReturn(audience);

        AsyncPlayerConnectionConfigureEvent event = mock(AsyncPlayerConnectionConfigureEvent.class);
        given(event.getConnection()).willReturn(connection);

        listener.onPlayerConfigure(event);

        verify(preJoinDialogService).markSkipPostJoinDialog(SESSION_ID);
        verifyNoInteractions(audience);
    }

    @Test
    public void shouldNotSkipPreJoinDialogsForUnverifiedPremiumPlayer() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        PlayerCache playerCache = mock(PlayerCache.class);
        DataSource dataSource = mock(DataSource.class);
        PreJoinDialogService preJoinDialogService = mock(PreJoinDialogService.class);
        SessionService sessionService = mock(SessionService.class);
        ProxySessionManager proxySessionManager = mock(ProxySessionManager.class);
        PremiumLoginVerifier premiumLoginVerifier = mock(PremiumLoginVerifier.class);
        setField(listener, "commonService", commonService);
        setField(listener, "playerCache", playerCache);
        setField(listener, "dataSource", dataSource);
        setField(listener, "preJoinDialogService", preJoinDialogService);
        setField(listener, "sessionService", sessionService);
        setField(listener, "proxySessionManager", proxySessionManager);
        setField(listener, "premiumLoginVerifier", premiumLoginVerifier);

        UUID premiumUuid = UUID.randomUUID();
        UUID playerId = UUID.randomUUID();
        PlayerAuth auth = PlayerAuth.builder()
            .name("bobby")
            .password(new HashedPassword("hash"))
            .premiumUuid(premiumUuid)
            .build();

        given(commonService.getProperty(PremiumSettings.ENABLE_PREMIUM)).willReturn(true);
        given(dataSource.getAuth("bobby")).willReturn(auth);
        given(premiumLoginVerifier.getVerifiedUuid("Bobby")).willReturn(null);  // not yet verified

        // UUID v4 that doesn't match stored premium UUID → must return false (impostor or wrong account)
        assertThat(invokeShouldSkipPreJoinDialogForPremium(listener, auth, "Bobby", playerId), is(false));
        verify(preJoinDialogService, never()).markSkipPostJoinDialog(anyLong());
    }

    @Test
    public void shouldSkipPreJoinDialogForPremiumPlayerWithOfflineUuidInProxyMode() throws Exception {
        // When the PlayerProfile at the configuration phase still has an offline UUID (v3) because
        // proxy forwarding hasn't been applied yet, the pre-join dialog must be skipped and the
        // final UUID check deferred to AsynchronousJoin.
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        PremiumLoginVerifier premiumLoginVerifier = mock(PremiumLoginVerifier.class);
        setField(listener, "commonService", commonService);
        setField(listener, "premiumLoginVerifier", premiumLoginVerifier);

        UUID premiumUuid = UUID.fromString("12345678-1234-4234-b234-123456789abc"); // v4
        // UUID v3 = offline player UUID (NameBasedGenerator → md5 variant, version 3)
        UUID offlineUuid = UUID.fromString("7b6d7e2a-0000-3000-8000-000000000001"); // v3
        PlayerAuth auth = PlayerAuth.builder()
            .name("bobby")
            .password(new HashedPassword("hash"))
            .premiumUuid(premiumUuid)
            .build();

        given(commonService.getProperty(PremiumSettings.ENABLE_PREMIUM)).willReturn(true);
        given(premiumLoginVerifier.getVerifiedUuid("Bobby")).willReturn(null); // PacketEvents not active

        assertThat(invokeShouldSkipPreJoinDialogForPremium(listener, auth, "Bobby", offlineUuid), is(true));
    }

    @Test
    public void shouldSkipPreJoinDialogForPremiumPlayerWithMatchingMojangUuidInProxyMode() throws Exception {
        // When the proxy has already forwarded the Mojang UUID (v4) into the PlayerProfile,
        // we can verify directly at the pre-join phase.
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        setField(listener, "commonService", commonService);

        UUID premiumUuid = UUID.randomUUID(); // v4 random
        PlayerAuth auth = PlayerAuth.builder()
            .name("bobby")
            .password(new HashedPassword("hash"))
            .premiumUuid(premiumUuid)
            .build();

        given(commonService.getProperty(PremiumSettings.ENABLE_PREMIUM)).willReturn(true);

        // Profile already has the Mojang UUID (v4) — verify returns true
        assertThat(invokeShouldSkipPreJoinDialogForPremium(listener, auth, "Bobby", premiumUuid), is(true));
    }

    @Test
    public void shouldNotSkipPreJoinDialogForImpostorWithMismatchedMojangUuidInProxyMode() throws Exception {
        // An impostor with a different Mojang UUID (v4) must NOT bypass the dialog.
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        setField(listener, "commonService", commonService);

        UUID storedPremiumUuid = UUID.randomUUID(); // v4 — the legitimate player's UUID
        UUID impostorUuid = UUID.randomUUID();      // v4 — a different Mojang account
        PlayerAuth auth = PlayerAuth.builder()
            .name("bobby")
            .password(new HashedPassword("hash"))
            .premiumUuid(storedPremiumUuid)
            .build();

        given(commonService.getProperty(PremiumSettings.ENABLE_PREMIUM)).willReturn(true);

        assertThat(invokeShouldSkipPreJoinDialogForPremium(listener, auth, "Bobby", impostorUuid), is(false));
    }

    private static boolean invokeShouldSkipPreJoinDialogForPremium(PaperDialogFlowListener listener,
                                                                   PlayerAuth auth, String playerName, UUID playerId) throws ReflectiveOperationException {
        var method = PaperDialogFlowListener.class
            .getDeclaredMethod("shouldSkipPreJoinDialogForPremium", PlayerAuth.class, String.class, UUID.class);
        method.setAccessible(true);
        return (boolean) method.invoke(listener, auth, playerName, playerId);
    }

    @Test
    public void shouldRetireSessionOfClosedConnectionOnly() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        PreJoinDialogService preJoinDialogService = mock(PreJoinDialogService.class);
        setField(listener, "preJoinDialogService", preJoinDialogService);

        PlayerConfigurationConnection gone = mockConnection("Bobby");
        given(gone.isConnected()).willReturn(false);
        PlayerConfigurationConnection live = mockConnection("Bobby");
        given(live.isConnected()).willReturn(true);
        CompletableFuture<String> liveFuture = new CompletableFuture<>();
        connectionSessions(listener).put(gone, SESSION_ID);
        connectionSessions(listener).put(live, SESSION_ID + 1);
        pendingResponses(listener, "pendingLoginResponses").put(SESSION_ID, new CompletableFuture<>());
        pendingResponses(listener, "pendingLoginResponses").put(SESSION_ID + 1, liveFuture);

        listener.onPlayerConnectionClose(new PlayerConnectionCloseEvent(
            UUID.randomUUID(), "Bobby", InetAddress.getLoopbackAddress(), false));

        verify(preJoinDialogService).retireSession(SESSION_ID);
        verify(preJoinDialogService, never()).retireSession(SESSION_ID + 1);
        assertThat(connectionSessions(listener).containsKey(gone), is(false));
        assertThat(connectionSessions(listener).get(live), is(SESSION_ID + 1));
        assertThat(pendingResponses(listener, "pendingLoginResponses").get(SESSION_ID + 1), is(liveFuture));
    }

    private static PlayerConfigurationConnection mockConnection(String playerName) {
        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getName()).willReturn(playerName);
        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        given(connection.getProfile()).willReturn(profile);
        return connection;
    }

    private static void seedSession(PaperDialogFlowListener listener, PlayerConfigurationConnection connection,
                                    String responseField, CompletableFuture<String> future) throws Exception {
        connectionSessions(listener).put(connection, SESSION_ID);
        pendingResponses(listener, responseField).put(SESSION_ID, future);
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentMap<PlayerConfigurationConnection, Long> connectionSessions(
        PaperDialogFlowListener listener) throws ReflectiveOperationException {
        Field field = PaperDialogFlowListener.class.getDeclaredField("connectionSessions");
        field.setAccessible(true);
        return (ConcurrentMap<PlayerConfigurationConnection, Long>) field.get(listener);
    }

    @SuppressWarnings("unchecked")
    private static ConcurrentMap<Long, CompletableFuture<String>> pendingResponses(
        PaperDialogFlowListener listener, String responseField) throws ReflectiveOperationException {
        Field field = PaperDialogFlowListener.class.getDeclaredField(responseField);
        field.setAccessible(true);
        return (ConcurrentMap<Long, CompletableFuture<String>>) field.get(listener);
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
