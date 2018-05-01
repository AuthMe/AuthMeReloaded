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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Function;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link AsyncRegister}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncRegisterTest {

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
    public void shouldDetectAlreadyLoggedInPlayer() {
        // given
        String name = "robert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(true);
        RegistrationExecutor executor = mock(RegistrationExecutor.class);
        singletonStoreWillReturn(registrationExecutorStore, executor);

        // when
        asyncRegister.register(RegistrationMethod.PASSWORD_REGISTRATION, PasswordRegisterParams.of(player, "abc", null));

        // then
        verify(commonService).send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
        verifyZeroInteractions(dataSource, executor);
    }

    @Test
    public void shouldStopForDisabledRegistration() {
        // given
        String name = "albert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(false);
        RegistrationExecutor executor = mock(RegistrationExecutor.class);
        singletonStoreWillReturn(registrationExecutorStore, executor);

        // when
        asyncRegister.register(RegistrationMethod.TWO_FACTOR_REGISTRATION, TwoFactorRegisterParams.of(player));

        // then
        verify(commonService).send(player, MessageKey.REGISTRATION_DISABLED);
        verifyZeroInteractions(dataSource, executor);
    }

    @Test
    public void shouldStopForAlreadyRegisteredName() {
        // given
        String name = "dilbert";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(true);
        given(dataSource.isAuthAvailable(name)).willReturn(true);
        RegistrationExecutor executor = mock(RegistrationExecutor.class);
        singletonStoreWillReturn(registrationExecutorStore, executor);

        // when
        asyncRegister.register(RegistrationMethod.TWO_FACTOR_REGISTRATION, TwoFactorRegisterParams.of(player));

        // then
        verify(commonService).send(player, MessageKey.NAME_ALREADY_REGISTERED);
        verify(dataSource, only()).isAuthAvailable(name);
        verifyZeroInteractions(executor);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldStopForCanceledEvent() {
        // given
        String name = "edbert";
        Player player = mockPlayerWithName(name);
        TestHelper.mockPlayerIp(player, "33.44.55.66");
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.IS_ENABLED)).willReturn(true);
        given(dataSource.isAuthAvailable(name)).willReturn(false);
        RegistrationExecutor executor = mock(RegistrationExecutor.class);
        TwoFactorRegisterParams params = TwoFactorRegisterParams.of(player);
        singletonStoreWillReturn(registrationExecutorStore, executor);

        AuthMeAsyncPreRegisterEvent canceledEvent = new AuthMeAsyncPreRegisterEvent(player, true);
        canceledEvent.setCanRegister(false);
        given(bukkitService.createAndCallEvent(any(Function.class))).willReturn(canceledEvent);

        // when
        asyncRegister.register(RegistrationMethod.TWO_FACTOR_REGISTRATION, params);

        // then
        verify(dataSource, only()).isAuthAvailable(name);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldStopForFailedExecutorCheck() {
        // given
        String name = "edbert";
        Player player = mockPlayerWithName(name);
        TestHelper.mockPlayerIp(player, "33.44.55.66");
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
