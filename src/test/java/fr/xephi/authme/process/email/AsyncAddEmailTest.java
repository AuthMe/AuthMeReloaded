package fr.xephi.authme.process.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.entity.Player;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link AsyncAddEmail}.
 */
public class AsyncAddEmailTest {

    private Messages messages;
    private Player player;
    private DataSource dataSource;
    private PlayerCache playerCache;

    @BeforeClass
    public static void setUp() {
        WrapperMock.createInstance();
    }

    // Clean up the fields to ensure that no test uses elements of another test
    @After
    public void removeFieldValues() {
        messages = null;
        player = null;
        dataSource = null;
        playerCache = null;
    }

    @Test
    public void shouldAddEmail() {
        // given
        AsyncAddEmail process = createProcess("my.mail@example.org");
        given(player.getName()).willReturn("testEr");
        given(playerCache.isAuthenticated("tester")).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(auth.getEmail()).willReturn(null);
        given(playerCache.getAuth("tester")).willReturn(auth);
        given(dataSource.isEmailStored("my.mail@example.org")).willReturn(false);

        // when
        process.process();

        // then
        verify(messages).send(player, MessageKey.EMAIL_ADDED_SUCCESS);
        verify(auth).setEmail("my.mail@example.org");
        verify(playerCache).updatePlayer(auth);
    }

    @Test
    public void shouldNotAddMailIfPlayerAlreadyHasEmail() {
        // given
        AsyncAddEmail process = createProcess("some.mail@example.org");
        given(player.getName()).willReturn("my_Player");
        given(playerCache.isAuthenticated("my_player")).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(auth.getEmail()).willReturn("another@mail.tld");
        given(playerCache.getAuth("my_player")).willReturn(auth);
        given(dataSource.isEmailStored("some.mail@example.org")).willReturn(false);

        // when
        process.process();

        // then
        verify(messages).send(player, MessageKey.USAGE_CHANGE_EMAIL);
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
    }

    @Test
    public void shouldNotAddMailIfItIsInvalid() {
        // given
        AsyncAddEmail process = createProcess("invalid_mail");
        given(player.getName()).willReturn("my_Player");
        given(playerCache.isAuthenticated("my_player")).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(auth.getEmail()).willReturn(null);
        given(playerCache.getAuth("my_player")).willReturn(auth);
        given(dataSource.isEmailStored("invalid_mail")).willReturn(false);

        // when
        process.process();

        // then
        verify(messages).send(player, MessageKey.INVALID_EMAIL);
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
    }

    @Test
    public void shouldNotAddMailIfAlreadyUsed() {
        // given
        AsyncAddEmail process = createProcess("player@mail.tld");
        given(player.getName()).willReturn("TestName");
        given(playerCache.isAuthenticated("testname")).willReturn(true);
        PlayerAuth auth = mock(PlayerAuth.class);
        given(auth.getEmail()).willReturn(null);
        given(playerCache.getAuth("testname")).willReturn(auth);
        given(dataSource.isEmailStored("player@mail.tld")).willReturn(true);

        // when
        process.process();

        // then
        verify(messages).send(player, MessageKey.EMAIL_ALREADY_USED_ERROR);
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
    }

    @Test
    public void shouldShowLoginMessage() {
        // given
        AsyncAddEmail process = createProcess("test@mail.com");
        given(player.getName()).willReturn("Username12");
        given(playerCache.isAuthenticated("username12")).willReturn(false);
        given(dataSource.isAuthAvailable("Username12")).willReturn(true);

        // when
        process.process();

        // then
        verify(messages).send(player, MessageKey.LOGIN_MESSAGE);
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
    }

    @Test
    public void shouldShowEmailRegisterMessage() {
        // given
        AsyncAddEmail process = createProcess("test@mail.com");
        given(player.getName()).willReturn("user");
        given(playerCache.isAuthenticated("user")).willReturn(false);
        given(dataSource.isAuthAvailable("user")).willReturn(false);
        Settings.emailRegistration = true;

        // when
        process.process();

        // then
        verify(messages).send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
    }

    @Test
    public void shouldShowRegularRegisterMessage() {
        // given
        AsyncAddEmail process = createProcess("test@mail.com");
        given(player.getName()).willReturn("user");
        given(playerCache.isAuthenticated("user")).willReturn(false);
        given(dataSource.isAuthAvailable("user")).willReturn(false);
        Settings.emailRegistration = false;

        // when
        process.process();

        // then
        verify(messages).send(player, MessageKey.REGISTER_MESSAGE);
        verify(playerCache, never()).updatePlayer(any(PlayerAuth.class));
    }

    /**
     * Create an instance of {@link AsyncAddEmail} and save the mcoks to this class' fields.
     *
     * @param email The email to use
     * @return The created process
     */
    private AsyncAddEmail createProcess(String email) {
        messages = mock(Messages.class);
        AuthMe authMe = mock(AuthMe.class);
        when(authMe.getMessages()).thenReturn(messages);
        player = mock(Player.class);
        dataSource = mock(DataSource.class);
        playerCache = mock(PlayerCache.class);
        return new AsyncAddEmail(authMe, player, email, dataSource, playerCache);
    }

}
