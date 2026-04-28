package fr.xephi.authme.listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.platform.PaperDialogActionKeys;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PreJoinDialogService;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.event.connection.configuration.AsyncPlayerConnectionConfigureEvent;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import net.kyori.adventure.audience.Audience;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class PaperDialogFlowListenerTest {

    @Test
    public void shouldListenLateOnPlayerConfigure() throws Exception {
        Method method = PaperDialogFlowListener.class
            .getDeclaredMethod("onPlayerConfigure", AsyncPlayerConnectionConfigureEvent.class);

        EventHandler annotation = method.getAnnotation(EventHandler.class);

        assertThat(annotation.priority(), is(EventPriority.HIGHEST));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldCompletePendingRegisterResponseForIncompleteSubmission() throws Exception {
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

        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.PASSWORD);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT))
            .willReturn(RegisterSecondaryArgument.CONFIRMATION);

        UUID playerId = UUID.randomUUID();
        CompletableFuture<String> future = new CompletableFuture<>();
        Field pendingField = PaperDialogFlowListener.class.getDeclaredField("pendingRegisterResponses");
        pendingField.setAccessible(true);
        ConcurrentMap<UUID, CompletableFuture<String>> pendingRegisterResponses =
            (ConcurrentMap<UUID, CompletableFuture<String>>) pendingField.get(listener);
        pendingRegisterResponses.put(playerId, future);

        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getId()).willReturn(playerId);
        given(profile.getName()).willReturn("Bobby");

        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        given(connection.getProfile()).willReturn(profile);

        DialogResponseView responseView = mock(DialogResponseView.class);
        given(responseView.getText("password")).willReturn("");

        PlayerCustomClickEvent event = mock(PlayerCustomClickEvent.class);
        given(event.getCommonConnection()).willReturn(connection);
        given(event.getIdentifier()).willReturn(PaperDialogActionKeys.PRE_JOIN_REGISTER_SUBMIT);
        given(event.getDialogResponseView()).willReturn(responseView);

        listener.onPlayerCustomClick(event);

        assertThat(future.isDone(), is(true));
        assertThat(future.getNow("sentinel"), is((String) null));
        verify(preJoinDialogService, never()).storePendingPasswordRegistration(
            org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldFallbackToPostJoinDialogWhenPreJoinRegisterIsCancelled() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        setField(listener, "commonService", commonService);
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_REGISTER_CANCEL_KICKS)).willReturn(false);

        UUID playerId = UUID.randomUUID();
        CompletableFuture<String> future = new CompletableFuture<>();
        Field pendingField = PaperDialogFlowListener.class.getDeclaredField("pendingRegisterResponses");
        pendingField.setAccessible(true);
        ConcurrentMap<UUID, CompletableFuture<String>> pendingRegisterResponses =
            (ConcurrentMap<UUID, CompletableFuture<String>>) pendingField.get(listener);
        pendingRegisterResponses.put(playerId, future);

        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getId()).willReturn(playerId);
        given(profile.getName()).willReturn("Bobby");

        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        given(connection.getProfile()).willReturn(profile);

        PlayerCustomClickEvent event = mock(PlayerCustomClickEvent.class);
        given(event.getCommonConnection()).willReturn(connection);
        given(event.getIdentifier()).willReturn(PaperDialogActionKeys.PRE_JOIN_REGISTER_CANCEL);

        listener.onPlayerCustomClick(event);

        assertThat(future.isDone(), is(true));
        assertThat(future.getNow("sentinel"), is((String) null));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldKickWhenPreJoinRegisterIsCancelledAndSettingEnabled() throws Exception {
        PaperDialogFlowListener listener = new PaperDialogFlowListener();
        CommonService commonService = mock(CommonService.class);
        Messages messages = mock(Messages.class);
        setField(listener, "commonService", commonService);
        setField(listener, "messages", messages);
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_REGISTER_CANCEL_KICKS)).willReturn(true);
        given(messages.retrieveSingle("Bobby", MessageKey.LOGIN_TIMEOUT_ERROR)).willReturn("Timed out!");

        UUID playerId = UUID.randomUUID();
        CompletableFuture<String> future = new CompletableFuture<>();
        Field pendingField = PaperDialogFlowListener.class.getDeclaredField("pendingRegisterResponses");
        pendingField.setAccessible(true);
        ConcurrentMap<UUID, CompletableFuture<String>> pendingRegisterResponses =
            (ConcurrentMap<UUID, CompletableFuture<String>>) pendingField.get(listener);
        pendingRegisterResponses.put(playerId, future);

        PlayerProfile profile = mock(PlayerProfile.class);
        given(profile.getId()).willReturn(playerId);
        given(profile.getName()).willReturn("Bobby");

        PlayerConfigurationConnection connection = mock(PlayerConfigurationConnection.class);
        given(connection.getProfile()).willReturn(profile);

        PlayerCustomClickEvent event = mock(PlayerCustomClickEvent.class);
        given(event.getCommonConnection()).willReturn(connection);
        given(event.getIdentifier()).willReturn(PaperDialogActionKeys.PRE_JOIN_REGISTER_CANCEL);

        listener.onPlayerCustomClick(event);

        assertThat(future.isDone(), is(true));
        assertThat(future.getNow(null), is("Timed out!"));
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

        verify(preJoinDialogService).clear(playerId);
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

        verify(preJoinDialogService).clear(playerId);
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

        verify(preJoinDialogService).clear(playerId);
        verifyNoInteractions(audience);
        verifyNoInteractions(sessionService);
    }

    private static void setField(Object target, String fieldName, Object value) throws ReflectiveOperationException {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
