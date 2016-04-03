package fr.xephi.authme.process.email;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AsyncChangeEmail}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncChangeEmailTest {

    @Mock
    private Player player;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private DataSource dataSource;
    @Mock
    private ProcessService service;
    @Mock
    private NewSetting settings;

    @Test
    public void shouldAddEmail() {
        // given
        AsyncChangeEmail process = createProcess("old@mail.tld", "new@mail.tld");
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail("old@mail.tld");
        given(playerCache.getAuth("bobby")).willReturn(auth);
        given(dataSource.updateEmail(auth)).willReturn(true);

        // when
        process.run();

        // then
        verify(dataSource).updateEmail(auth);
        verify(playerCache).updatePlayer(auth);
        verify(service).send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
    }

    @Test
    public void shouldShowErrorIfSaveFails() {
        // given
        AsyncChangeEmail process = createProcess("old@mail.tld", "new@mail.tld");
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail("old@mail.tld");
        given(playerCache.getAuth("bobby")).willReturn(auth);
        given(dataSource.updateEmail(auth)).willReturn(false);

        // when
        process.run();

        // then
        verify(dataSource).updateEmail(auth);
        verify(playerCache, never()).updatePlayer(auth);
        verify(service).send(player, MessageKey.ERROR);
    }

    @Test
    public void shouldShowAddEmailUsage() {
        // given
        AsyncChangeEmail process = createProcess("old@mail.tld", "new@mail.tld");
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail(null);
        given(playerCache.getAuth("bobby")).willReturn(auth);

        // when
        process.run();

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.USAGE_ADD_EMAIL);
    }

    @Test
    public void shouldRejectInvalidNewMail() {
        // given
        AsyncChangeEmail process = createProcess("old@mail.tld", "bogus");
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail("old@mail.tld");
        given(playerCache.getAuth("bobby")).willReturn(auth);

        // when
        process.run();

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.INVALID_NEW_EMAIL);
    }

    @Test
    public void shouldRejectInvalidOldEmail() {
        // given
        AsyncChangeEmail process = createProcess("old@mail.tld", "new@mail.tld");
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail("other@address.email");
        given(playerCache.getAuth("bobby")).willReturn(auth);

        // when
        process.run();

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.INVALID_OLD_EMAIL);
    }

    @Test
    public void shouldRejectAlreadyUsedEmail() {
        // given
        AsyncChangeEmail process = createProcess("old@example.com", "new@example.com");
        given(player.getName()).willReturn("Username");
        given(playerCache.isAuthenticated("username")).willReturn(true);
        PlayerAuth auth = authWithMail("old@example.com");
        given(playerCache.getAuth("username")).willReturn(auth);
        given(dataSource.countAuthsByEmail("new@example.com")).willReturn(5);

        // when
        process.run();

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.EMAIL_ALREADY_USED_ERROR);
    }

    @Test
    public void shouldSendLoginMessage() {
        // given
        AsyncChangeEmail process = createProcess("old@mail.tld", "new@mail.tld");
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(false);
        given(dataSource.isAuthAvailable("Bobby")).willReturn(true);

        // when
        process.run();

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.LOGIN_MESSAGE);
    }

    @Test
    public void shouldShowEmailRegistrationMessage() {
        // given
        AsyncChangeEmail process = createProcess("old@mail.tld", "new@mail.tld");
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(false);
        given(dataSource.isAuthAvailable("Bobby")).willReturn(false);
        given(service.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);

        // when
        process.run();

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
    }

    @Test
    public void shouldShowRegistrationMessage() {
        // given
        AsyncChangeEmail process = createProcess("old@mail.tld", "new@mail.tld");
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(false);
        given(dataSource.isAuthAvailable("Bobby")).willReturn(false);
        given(service.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(false);

        // when
        process.run();

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.REGISTER_MESSAGE);
    }

    private static PlayerAuth authWithMail(String email) {
        PlayerAuth auth = mock(PlayerAuth.class);
        when(auth.getEmail()).thenReturn(email);
        return auth;
    }

    private AsyncChangeEmail createProcess(String oldEmail, String newEmail) {
        given(service.getProperty(EmailSettings.MAX_REG_PER_EMAIL)).willReturn(5);
        given(service.getSettings()).willReturn(settings);
        return new AsyncChangeEmail(player, oldEmail, newEmail, dataSource, playerCache, service);
    }
}
