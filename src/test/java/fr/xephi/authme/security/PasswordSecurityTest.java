package fr.xephi.authme.security;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.JOOMLA;
import fr.xephi.authme.security.crypts.PHPBB;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link PasswordSecurity}.
 */
public class PasswordSecurityTest {

    private PluginManager pluginManager;
    private DataSource dataSource;
    private EncryptionMethod method;
    private Class<?> caughtClassInEvent;

    @Before
    public void setUpMocks() {
        WrapperMock.createInstance();
        pluginManager = mock(PluginManager.class);
        dataSource = mock(DataSource.class);
        method = mock(EncryptionMethod.class);
        caughtClassInEvent = null;

        // When the password encryption event is emitted, replace the encryption method with our mock.
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                if (arguments[0] instanceof PasswordEncryptionEvent) {
                    PasswordEncryptionEvent event = (PasswordEncryptionEvent) arguments[0];
                    caughtClassInEvent = event.getMethod() != null ? event.getMethod().getClass() : null;
                    event.setMethod(method);
                }
                return null;
            }
        }).when(pluginManager).callEvent(any(Event.class));
    }

    @Test
    public void shouldReturnPasswordMatch() {
        // given
        HashedPassword password = new HashedPassword("$TEST$10$SOME_HASH", null);
        String playerName = "Tester";
        // Calls to EncryptionMethod are always with the lower-case version of the name
        String playerLowerCase = playerName.toLowerCase();
        String clearTextPass = "myPassTest";

        given(dataSource.getPassword(playerName)).willReturn(password);
        given(method.comparePassword(clearTextPass, password, playerLowerCase)).willReturn(true);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.BCRYPT, pluginManager, false);

        // when
        boolean result = security.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(true));
        verify(dataSource).getPassword(playerName);
        verify(pluginManager).callEvent(any(PasswordEncryptionEvent.class));
        verify(method).comparePassword(clearTextPass, password, playerLowerCase);
    }

    @Test
    public void shouldReturnPasswordMismatch() {
        // given
        HashedPassword password = new HashedPassword("$TEST$10$SOME_HASH", null);
        String playerName = "My_PLayer";
        String playerLowerCase = playerName.toLowerCase();
        String clearTextPass = "passw0Rd1";

        PlayerAuth auth = mock(PlayerAuth.class);
        given(dataSource.getPassword(playerName)).willReturn(password);
        given(method.comparePassword(clearTextPass, password, playerLowerCase)).willReturn(false);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.CUSTOM, pluginManager, false);

        // when
        boolean result = security.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource).getPassword(playerName);
        verify(pluginManager).callEvent(any(PasswordEncryptionEvent.class));
        verify(method).comparePassword(clearTextPass, password, playerLowerCase);
    }

    @Test
    public void shouldReturnFalseIfPlayerDoesNotExist() {
        // given
        String playerName = "bobby";
        String clearTextPass = "tables";

        given(dataSource.getPassword(playerName)).willReturn(null);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.MD5, pluginManager, false);

        // when
        boolean result = security.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource).getPassword(playerName);
        verify(pluginManager, never()).callEvent(any(Event.class));
        verify(method, never()).comparePassword(anyString(), any(HashedPassword.class), anyString());
    }

    @Test
    public void shouldTryOtherMethodsForFailedPassword() {
        // given
        // BCRYPT hash for "Test"
        HashedPassword password =
            new HashedPassword("$2y$10$2e6d2193f43501c926e25elvWlPmWczmrfrnbZV0dUZGITjYjnkkW");
        String playerName = "somePlayer";
        String playerLowerCase = playerName.toLowerCase();
        String clearTextPass = "Test";
        // MD5 hash for "Test"
        HashedPassword newPassword = new HashedPassword("0cbc6611f5540bd0809a388dc95a615b");

        given(dataSource.getPassword(argThat(equalToIgnoringCase(playerName)))).willReturn(password);
        given(method.comparePassword(clearTextPass, password, playerLowerCase)).willReturn(false);
        given(method.computeHash(clearTextPass, playerLowerCase)).willReturn(newPassword);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.MD5, pluginManager, true);

        // when
        boolean result = security.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(true));
        // Note ljacqu 20151230: We need to check the player name in a case-insensitive way because the methods within
        // PasswordSecurity may convert the name into all lower-case. This is desired because EncryptionMethod methods
        // should only be invoked with all lower-case names. Data source is case-insensitive itself, so this is fine.
        verify(dataSource).getPassword(argThat(equalToIgnoringCase(playerName)));
        verify(pluginManager, times(2)).callEvent(any(PasswordEncryptionEvent.class));
        verify(method).comparePassword(clearTextPass, password, playerLowerCase);
        verify(dataSource).updatePassword(playerLowerCase, newPassword);
    }

    @Test
    public void shouldHashPassword() {
        // given
        String password = "MyP@ssword";
        String username = "theUserInTest";
        String usernameLowerCase = username.toLowerCase();
        HashedPassword hashedPassword = new HashedPassword("$T$est#Hash", "__someSalt__");
        given(method.computeHash(password, usernameLowerCase)).willReturn(hashedPassword);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.JOOMLA, pluginManager, true);

        // when
        HashedPassword result = security.computeHash(password, username);

        // then
        assertThat(result, equalTo(hashedPassword));
        ArgumentCaptor<PasswordEncryptionEvent> captor = ArgumentCaptor.forClass(PasswordEncryptionEvent.class);
        verify(pluginManager).callEvent(captor.capture());
        PasswordEncryptionEvent event = captor.getValue();
        assertThat(JOOMLA.class.equals(caughtClassInEvent), equalTo(true));
        assertThat(event.getPlayerName(), equalTo(usernameLowerCase));
    }

    @Test
    public void shouldHashPasswordWithGivenAlgorithm() {
        // given
        String password = "TopSecretPass#112525";
        String username = "someone12";
        HashedPassword hashedPassword = new HashedPassword("~T!est#Hash", "__someSalt__");
        given(method.computeHash(password, username)).willReturn(hashedPassword);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.JOOMLA, pluginManager, true);

        // when
        HashedPassword result = security.computeHash(HashAlgorithm.PHPBB, password, username);

        // then
        assertThat(result, equalTo(hashedPassword));
        ArgumentCaptor<PasswordEncryptionEvent> captor = ArgumentCaptor.forClass(PasswordEncryptionEvent.class);
        verify(pluginManager).callEvent(captor.capture());
        PasswordEncryptionEvent event = captor.getValue();
        assertThat(PHPBB.class.equals(caughtClassInEvent), equalTo(true));
        assertThat(event.getPlayerName(), equalTo(username));
    }

    @Test
    public void shouldSkipCheckIfMandatorySaltIsUnavailable() {
        // given
        String password = "?topSecretPass\\";
        String username = "someone12";
        HashedPassword hashedPassword = new HashedPassword("~T!est#Hash");
        given(method.computeHash(password, username)).willReturn(hashedPassword);
        given(method.hasSeparateSalt()).willReturn(true);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.XAUTH, pluginManager, false);

        // when
        boolean result = security.comparePassword(password, hashedPassword, username);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource, never()).getAuth(anyString());
        verify(pluginManager).callEvent(any(PasswordEncryptionEvent.class));
        verify(method, never()).comparePassword(anyString(), any(HashedPassword.class), anyString());
    }

}
