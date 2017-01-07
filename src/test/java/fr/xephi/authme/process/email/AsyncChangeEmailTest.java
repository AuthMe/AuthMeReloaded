package fr.xephi.authme.process.email;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AsyncChangeEmail}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AsyncChangeEmailTest {

    @InjectMocks
    private AsyncChangeEmail process;

    @Mock
    private Player player;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private DataSource dataSource;

    @Mock
    private CommonService service;

    @Mock
    private ValidationService validationService;

    @Test
    public void shouldAddEmail() {
        // given
        String newEmail = "new@mail.tld";
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail("old@mail.tld");
        given(playerCache.getAuth("bobby")).willReturn(auth);
        given(dataSource.updateEmail(auth)).willReturn(true);
        given(validationService.validateEmail(newEmail)).willReturn(true);
        given(validationService.isEmailFreeForRegistration(newEmail, player)).willReturn(true);

        // when
        process.changeEmail(player, "old@mail.tld", newEmail);

        // then
        verify(dataSource).updateEmail(auth);
        verify(playerCache).updatePlayer(auth);
        verify(service).send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
    }

    @Test
    public void shouldShowErrorIfSaveFails() {
        // given
        String newEmail = "new@mail.tld";
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail("old@mail.tld");
        given(playerCache.getAuth("bobby")).willReturn(auth);
        given(dataSource.updateEmail(auth)).willReturn(false);
        given(validationService.validateEmail(newEmail)).willReturn(true);
        given(validationService.isEmailFreeForRegistration(newEmail, player)).willReturn(true);

        // when
        process.changeEmail(player, "old@mail.tld", newEmail);

        // then
        verify(dataSource).updateEmail(auth);
        verify(playerCache, never()).updatePlayer(auth);
        verify(service).send(player, MessageKey.ERROR);
    }

    @Test
    public void shouldShowAddEmailUsage() {
        // given
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail(null);
        given(playerCache.getAuth("bobby")).willReturn(auth);

        // when
        process.changeEmail(player, "old@mail.tld", "new@mailt.tld");

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.USAGE_ADD_EMAIL);
    }

    @Test
    public void shouldRejectInvalidNewMail() {
        // given
        String newEmail = "bogus";
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail("old@mail.tld");
        given(playerCache.getAuth("bobby")).willReturn(auth);
        given(validationService.validateEmail(newEmail)).willReturn(false);

        // when
        process.changeEmail(player, "old@mail.tld", newEmail);

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.INVALID_NEW_EMAIL);
    }

    @Test
    public void shouldRejectInvalidOldEmail() {
        // given
        String newEmail = "new@mail.tld";
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(true);
        PlayerAuth auth = authWithMail("other@address.email");
        given(playerCache.getAuth("bobby")).willReturn(auth);
        given(validationService.validateEmail(newEmail)).willReturn(true);

        // when
        process.changeEmail(player, "old@mail.tld", newEmail);

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.INVALID_OLD_EMAIL);
    }

    @Test
    public void shouldRejectAlreadyUsedEmail() {
        // given
        String newEmail = "new@example.com";
        given(player.getName()).willReturn("Username");
        given(playerCache.isAuthenticated("username")).willReturn(true);
        PlayerAuth auth = authWithMail("old@example.com");
        given(playerCache.getAuth("username")).willReturn(auth);
        given(validationService.validateEmail(newEmail)).willReturn(true);
        given(validationService.isEmailFreeForRegistration(newEmail, player)).willReturn(false);

        // when
        process.changeEmail(player, "old@example.com", newEmail);

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.EMAIL_ALREADY_USED_ERROR);
    }

    @Test
    public void shouldSendLoginMessage() {
        // given
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(false);
        given(dataSource.isAuthAvailable("Bobby")).willReturn(true);

        // when
        process.changeEmail(player, "old@mail.tld", "new@mail.tld");

        // then
        verify(dataSource, never()).updateEmail(any(PlayerAuth.class));
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
        verify(service).send(player, MessageKey.LOGIN_MESSAGE);
    }

    @Test
    public void shouldShowRegistrationMessage() {
        // given
        given(player.getName()).willReturn("Bobby");
        given(playerCache.isAuthenticated("bobby")).willReturn(false);
        given(dataSource.isAuthAvailable("Bobby")).willReturn(false);

        // when
        process.changeEmail(player, "old@mail.tld", "new@mail.tld");

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

}
