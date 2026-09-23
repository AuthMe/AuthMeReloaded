package fr.xephi.authme.process.register;

import ch.jalu.injector.factory.SingletonStore;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AuthMeAsyncPreRegisterEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.register.executors.PasswordRegisterParams;
import fr.xephi.authme.process.register.executors.RegistrationExecutor;
import fr.xephi.authme.process.register.executors.RegistrationMethod;
import fr.xephi.authme.process.register.executors.TwoFactorRegisterParams;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link AsyncRegister}.
 */
@ExtendWith(MockitoExtension.class)
class AsyncRegisterTest {

    @InjectMocks
    private AsyncRegister asyncRegister;

    @Mock
    private PlayerCache playerCache;
    @Mock
    private CommonService commonService;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private DataSource dataSource;
    @Mock
    private SingletonStore<RegistrationExecutor> registrationExecutorStore;

    @Test
    void shouldDetectAlreadyLoggedInPlayer() {
        // given
        String name = "robert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        asyncRegister.register(RegistrationMethod.PASSWORD_REGISTRATION, PasswordRegisterParams.of(player, "abc", null));

        // then
        verify(commonService).send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
        verifyNoInteractions(dataSource, registrationExecutorStore);
    }

    @Test
    void shouldStopForDisabledRegistration() {
        // given
        String name = "albert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(false);

        // when
        asyncRegister.register(RegistrationMethod.TWO_FACTOR_REGISTRATION, TwoFactorRegisterParams.of(player));

        // then
        verify(commonService).send(player, MessageKey.REGISTRATION_DISABLED);
        verifyNoInteractions(dataSource, registrationExecutorStore);
    }

    @Test
    void shouldStopForAlreadyRegisteredName() {
        // given
        String name = "dilbert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(true);
        given(dataSource.isAuthAvailable(name)).willReturn(true);

        // when
        asyncRegister.register(RegistrationMethod.TWO_FACTOR_REGISTRATION, TwoFactorRegisterParams.of(player));

        // then
        verify(commonService).send(player, MessageKey.NAME_ALREADY_REGISTERED);
        verify(dataSource, only()).isAuthAvailable(name);
        verifyNoInteractions(registrationExecutorStore);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldStopForCanceledEvent() {
        // given
        String name = "edbert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(true);
        given(dataSource.isAuthAvailable(name)).willReturn(false);
        TwoFactorRegisterParams params = TwoFactorRegisterParams.of(player);

        AuthMeAsyncPreRegisterEvent canceledEvent = new AuthMeAsyncPreRegisterEvent(player, true);
        canceledEvent.setCanRegister(false);
        given(bukkitService.createAndCallEvent(any(Function.class))).willReturn(canceledEvent);

        // when
        asyncRegister.register(RegistrationMethod.TWO_FACTOR_REGISTRATION, params);

        // then
        verify(dataSource, only()).isAuthAvailable(name);
        verifyNoInteractions(registrationExecutorStore);
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldStopForFailedExecutorCheck() {
        // given
        String name = "edbert";
        Player player = mockPlayerWithName(name);
        TestHelper.mockIpAddressToPlayer(player, "33.44.55.66");
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(true);
        given(commonService.getProperty(RestrictionSettings.MAX_REGISTRATION_PER_IP)).willReturn(0);
        given(dataSource.isAuthAvailable(name)).willReturn(false);
        RegistrationExecutor executor = mock(RegistrationExecutor.class);
        TwoFactorRegisterParams params = TwoFactorRegisterParams.of(player);
        given(executor.isRegistrationAdmitted(params)).willReturn(false);
        singletonStoreWillReturn(registrationExecutorStore, executor);

        given(bukkitService.createAndCallEvent(any(Function.class)))
            .willReturn(new AuthMeAsyncPreRegisterEvent(player, false));

        // when
        asyncRegister.register(RegistrationMethod.TWO_FACTOR_REGISTRATION, params);

        // then
        verify(dataSource, only()).isAuthAvailable(name);
        verify(executor, only()).isRegistrationAdmitted(params);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

    @SuppressWarnings("unchecked")
    private static void singletonStoreWillReturn(SingletonStore<RegistrationExecutor> store,
                                                 RegistrationExecutor mock) {
        given(store.getSingleton(any(Class.class))).willReturn(mock);
    }
}
