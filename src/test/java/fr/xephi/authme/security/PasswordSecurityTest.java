package fr.xephi.authme.security;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.EncryptedPassword;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.JOOMLA;
import fr.xephi.authme.security.crypts.PHPBB;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
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
        EncryptedPassword password = new EncryptedPassword("$TEST$10$SOME_HASH", null);
        String playerName = "Tester";
        String clearTextPass = "myPassTest";

        PlayerAuth auth = mock(PlayerAuth.class);
        given(auth.getPassword()).willReturn(password);
        given(dataSource.getAuth(playerName)).willReturn(auth);
        given(method.comparePassword(clearTextPass, password, playerName)).willReturn(true);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.BCRYPT, pluginManager, false);

        // when
        boolean result = security.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(true));
        verify(dataSource).getAuth(playerName);
        verify(pluginManager).callEvent(any(PasswordEncryptionEvent.class));
        verify(method).comparePassword(clearTextPass, password, playerName);
    }

    @Test
    public void shouldReturnPasswordMismatch() {
        // given
        EncryptedPassword password = new EncryptedPassword("$TEST$10$SOME_HASH", null);
        String playerName = "My_PLayer";
        String clearTextPass = "passw0Rd1";

        PlayerAuth auth = mock(PlayerAuth.class);
        given(auth.getPassword()).willReturn(password);
        given(dataSource.getAuth(playerName)).willReturn(auth);
        given(method.comparePassword(clearTextPass, password, playerName)).willReturn(false);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.CUSTOM, pluginManager, false);

        // when
        boolean result = security.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource).getAuth(playerName);
        verify(pluginManager).callEvent(any(PasswordEncryptionEvent.class));
        verify(method).comparePassword(clearTextPass, password, playerName);
    }

    @Test
    public void shouldReturnFalseIfPlayerDoesNotExist() {
        // given
        String playerName = "bobby";
        String clearTextPass = "tables";

        given(dataSource.getAuth(playerName)).willReturn(null);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.MD5, pluginManager, false);

        // when
        boolean result = security.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource).getAuth(playerName);
        verify(pluginManager, never()).callEvent(any(Event.class));
        verify(method, never()).comparePassword(anyString(), any(EncryptedPassword.class), anyString());
    }

    @Test
    public void shouldTryOtherMethodsForFailedPassword() {
        // given
        // BCRYPT2Y hash for "Test"
        EncryptedPassword password =
            new EncryptedPassword("$2y$10$2e6d2193f43501c926e25elvWlPmWczmrfrnbZV0dUZGITjYjnkkW");
        String playerName = "somePlayer";
        String clearTextPass = "Test";
        // MD5 hash for "Test"
        EncryptedPassword newPassword = new EncryptedPassword("0cbc6611f5540bd0809a388dc95a615b");

        PlayerAuth auth = mock(PlayerAuth.class);
        doCallRealMethod().when(auth).getPassword();
        doCallRealMethod().when(auth).setPassword(any(EncryptedPassword.class));
        auth.setPassword(password);
        given(dataSource.getAuth(playerName)).willReturn(auth);
        given(method.comparePassword(clearTextPass, password, playerName)).willReturn(false);
        given(method.computeHash(clearTextPass, playerName)).willReturn(newPassword);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.MD5, pluginManager, true);

        // when
        boolean result = security.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(true));
        verify(dataSource, times(2)).getAuth(playerName);
        verify(pluginManager, times(2)).callEvent(any(PasswordEncryptionEvent.class));
        verify(method).comparePassword(clearTextPass, password, playerName);
        verify(auth).setPassword(newPassword);

        ArgumentCaptor<PlayerAuth> captor = ArgumentCaptor.forClass(PlayerAuth.class);
        verify(dataSource).updatePassword(captor.capture());
        assertThat(captor.getValue().getPassword(), equalTo(newPassword));
    }

    @Test
    public void shouldHashPassword() {
        // given
        String password = "MyP@ssword";
        String username = "theUserInTest";
        EncryptedPassword encryptedPassword = new EncryptedPassword("$T$est#Hash", "__someSalt__");
        given(method.computeHash(password, username)).willReturn(encryptedPassword);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.JOOMLA, pluginManager, true);

        // when
        EncryptedPassword result = security.computeHash(password, username);

        // then
        assertThat(result, equalTo(encryptedPassword));
        ArgumentCaptor<PasswordEncryptionEvent> captor = ArgumentCaptor.forClass(PasswordEncryptionEvent.class);
        verify(pluginManager).callEvent(captor.capture());
        PasswordEncryptionEvent event = captor.getValue();
        assertThat(JOOMLA.class.equals(caughtClassInEvent), equalTo(true));
        assertThat(event.getPlayerName(), equalTo(username));
    }

    @Test
    public void shouldHashPasswordWithGivenAlgorithm() {
        // given
        String password = "TopSecretPass#112525";
        String username = "someone12";
        EncryptedPassword encryptedPassword = new EncryptedPassword("~T!est#Hash", "__someSalt__");
        given(method.computeHash(password, username)).willReturn(encryptedPassword);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.JOOMLA, pluginManager, true);

        // when
        EncryptedPassword result = security.computeHash(HashAlgorithm.PHPBB, password, username);

        // then
        assertThat(result, equalTo(encryptedPassword));
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
        EncryptedPassword encryptedPassword = new EncryptedPassword("~T!est#Hash");
        given(method.computeHash(password, username)).willReturn(encryptedPassword);
        given(method.hasSeparateSalt()).willReturn(true);
        PasswordSecurity security = new PasswordSecurity(dataSource, HashAlgorithm.XAUTH, pluginManager, true);

        // when
        boolean result = security.comparePassword(password, encryptedPassword, username);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource, never()).getAuth(anyString());
        verify(pluginManager).callEvent(any(PasswordEncryptionEvent.class));
        verify(method, never()).comparePassword(anyString(), any(EncryptedPassword.class), anyString());
    }

}
