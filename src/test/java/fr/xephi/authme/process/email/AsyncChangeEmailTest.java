package fr.xephi.authme.process.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AsyncChangeEmail}.
 */
public class AsyncChangeEmailTest {

    private Player player;
    private Messages messages;
    private PlayerCache playerCache;
    private DataSource dataSource;

    @BeforeClass
    public static void setUp() {
        WrapperMock.createInstance();
    }

    // Prevent the accidental re-use of a field in another test
    @After
    public void cleanFields() {
        player = null;
        messages = null;
        playerCache = null;
        dataSource = null;
    }

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
        process.process();

        // then
        verify(dataSource).updateEmail(auth);
        verify(playerCache).updatePlayer(auth);
        verify(messages).send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
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
        process.process();

        // then
        verify(dataSource, never()).updateEmail(auth);
        verify(playerCache, never()).updatePlayer(auth);
        verify(messages).send(player, MessageKey.USAGE_ADD_EMAIL);
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
        process.process();

        // then
        verify(dataSource, never()).updateEmail(auth);
        verify(playerCache, never()).updatePlayer(auth);
        verify(messages).send(player, MessageKey.INVALID_NEW_EMAIL);
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
        process.process();

        // then
        verify(dataSource, never()).updateEmail(auth);
        verify(playerCache, never()).updatePlayer(auth);
        verify(messages).send(player, MessageKey.INVALID_OLD_EMAIL);
    }

    private static PlayerAuth authWithMail(String email) {
        PlayerAuth auth = mock(PlayerAuth.class);
        when(auth.getEmail()).thenReturn(email);
        return auth;
    }

    private AsyncChangeEmail createProcess(String oldEmail, String newEmail) {
        player = mock(Player.class);
        messages = mock(Messages.class);
        AuthMe authMe = mock(AuthMe.class);
        when(authMe.getMessages()).thenReturn(messages);
        playerCache = mock(PlayerCache.class);
        dataSource = mock(DataSource.class);
        return new AsyncChangeEmail(player, authMe, oldEmail, newEmail, dataSource, playerCache);
    }
}
