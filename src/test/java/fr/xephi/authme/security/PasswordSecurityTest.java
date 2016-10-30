package fr.xephi.authme.security;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.JOOMLA;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link PasswordSecurity}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PasswordSecurityTest {

    private Injector injector;

    @Mock
    private Settings settings;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private DataSource dataSource;

    @Mock
    private EncryptionMethod method;

    private Class<?> caughtClassInEvent;

    @BeforeClass
    public static void setUpTest() {
        TestHelper.setupLogger();
    }

    @Before
    public void setUpMocks() {
        caughtClassInEvent = null;

        // When the password encryption event is emitted, replace the encryption method with our mock.
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                Object[] arguments = invocation.getArguments();
                if (arguments[0] instanceof PasswordEncryptionEvent) {
                    PasswordEncryptionEvent event = (PasswordEncryptionEvent) arguments[0];
                    caughtClassInEvent = event.getMethod() == null ? null : event.getMethod().getClass();
                    event.setMethod(method);
                }
                return null;
            }
        }).when(pluginManager).callEvent(any(Event.class));
        injector = new InjectorBuilder().addDefaultHandlers("fr.xephi.authme").create();
        injector.register(Settings.class, settings);
        injector.register(DataSource.class, dataSource);
        injector.register(PluginManager.class, pluginManager);
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
        initSettings(HashAlgorithm.BCRYPT, false);
        PasswordSecurity security = newPasswordSecurity();

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

        given(dataSource.getPassword(playerName)).willReturn(password);
        given(method.comparePassword(clearTextPass, password, playerLowerCase)).willReturn(false);
        initSettings(HashAlgorithm.CUSTOM, false);
        PasswordSecurity security = newPasswordSecurity();

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
        initSettings(HashAlgorithm.MD5, false);
        PasswordSecurity security = newPasswordSecurity();

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
        initSettings(HashAlgorithm.MD5, true);
        PasswordSecurity security = newPasswordSecurity();

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
    public void shouldTryAllMethodsAndFail() {
        // given
        HashedPassword password = new HashedPassword("hashNotMatchingAnyMethod", "someBogusSalt");
        String playerName = "asfd";
        String clearTextPass = "someInvalidPassword";
        given(dataSource.getPassword(playerName)).willReturn(password);
        given(method.comparePassword(clearTextPass, password, playerName)).willReturn(false);
        initSettings(HashAlgorithm.MD5, true);
        PasswordSecurity security = newPasswordSecurity();

        // when
        boolean result = security.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource, never()).updatePassword(anyString(), any(HashedPassword.class));
    }

    @Test
    public void shouldHashPassword() {
        // given
        String password = "MyP@ssword";
        String username = "theUserInTest";
        String usernameLowerCase = username.toLowerCase();
        HashedPassword hashedPassword = new HashedPassword("$T$est#Hash", "__someSalt__");
        given(method.computeHash(password, usernameLowerCase)).willReturn(hashedPassword);
        initSettings(HashAlgorithm.JOOMLA, true);
        PasswordSecurity security = newPasswordSecurity();

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
    public void shouldSkipCheckIfMandatorySaltIsUnavailable() {
        // given
        String password = "?topSecretPass\\";
        String username = "someone12";
        HashedPassword hashedPassword = new HashedPassword("~T!est#Hash");
        given(method.hasSeparateSalt()).willReturn(true);
        initSettings(HashAlgorithm.XAUTH, false);
        PasswordSecurity security = newPasswordSecurity();

        // when
        boolean result = security.comparePassword(password, hashedPassword, username);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource, never()).getAuth(anyString());
        verify(pluginManager).callEvent(any(PasswordEncryptionEvent.class));
        verify(method, never()).comparePassword(anyString(), any(HashedPassword.class), anyString());
    }

    @Test
    public void shouldReloadSettings() {
        // given
        initSettings(HashAlgorithm.BCRYPT, false);
        PasswordSecurity passwordSecurity = newPasswordSecurity();
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.MD5);
        given(settings.getProperty(SecuritySettings.SUPPORT_OLD_PASSWORD_HASH)).willReturn(true);

        // when
        passwordSecurity.reload();

        // then
        assertThat(ReflectionTestUtils.getFieldValue(PasswordSecurity.class, passwordSecurity, "algorithm"),
            equalTo((Object) HashAlgorithm.MD5));
        assertThat(ReflectionTestUtils.getFieldValue(PasswordSecurity.class, passwordSecurity, "supportOldAlgorithm"),
            equalTo((Object) Boolean.TRUE));
    }

    private PasswordSecurity newPasswordSecurity() {
        // Use this method to make sure we have all dependents of PasswordSecurity already registered as mocks
        PasswordSecurity passwordSecurity = injector.createIfHasDependencies(PasswordSecurity.class);
        if (passwordSecurity == null) {
            throw new IllegalStateException("Cannot create PasswordSecurity directly! "
                + "Did you forget to provide a dependency as mock?");
        }
        return passwordSecurity;
    }

    private void initSettings(HashAlgorithm algorithm, boolean supportOldPassword) {
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(algorithm);
        given(settings.getProperty(SecuritySettings.SUPPORT_OLD_PASSWORD_HASH)).willReturn(supportOldPassword);
        given(settings.getProperty(HooksSettings.BCRYPT_LOG2_ROUND)).willReturn(8);
        given(settings.getProperty(SecuritySettings.DOUBLE_MD5_SALT_LENGTH)).willReturn(16);
    }

}
