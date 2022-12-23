package fr.xephi.authme.security;

import ch.jalu.injector.Injector;
import ch.jalu.injector.InjectorBuilder;
import ch.jalu.injector.factory.Factory;
import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.Joomla;
import fr.xephi.authme.security.crypts.Md5;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.event.Event;
import org.bukkit.plugin.PluginManager;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link PasswordSecurity}.
 */
@RunWith(DelayedInjectionRunner.class)
public class PasswordSecurityTest {

    @InjectDelayed
    private PasswordSecurity passwordSecurity;

    @Mock
    private Settings settings;

    @Mock
    private PluginManager pluginManager;

    @Mock
    private DataSource dataSource;

    @Mock
    private Factory<HashAlgorithm> hashAlgorithmFactory;

    @Mock
    private EncryptionMethod method;

    private Class<?> caughtClassInEvent;

    @BeforeClass
    public static void setUpTest() {
        TestHelper.setupLogger();
    }

    @BeforeInjecting
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

        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.BCRYPT);
        given(settings.getProperty(SecuritySettings.LEGACY_HASHES)).willReturn(Collections.emptySet());
        given(settings.getProperty(HooksSettings.BCRYPT_LOG2_ROUND)).willReturn(8);

        Injector injector = new InjectorBuilder()
            .addDefaultHandlers("fr.xephi.authme.security.crypts")
            .create();
        injector.register(Settings.class, settings);

        given(hashAlgorithmFactory.newInstance(any(Class.class))).willAnswer(invocation -> {
                Object o = injector.createIfHasDependencies(invocation.getArgument(0));
                if (o == null) {
                    throw new IllegalArgumentException("Cannot create object of class '" + invocation.getArgument(0)
                        + "': missing class that needs to be provided?");
                }
                return o;
        });
    }

    @Test
    public void shouldReturnPasswordMatch() {
        // given
        HashedPassword password = new HashedPassword("$TEST$10$SOME_HASH", null);
        String playerName = "Tester";
        // Calls to EncryptionMethod are always with the lower-case version of the name
        String clearTextPass = "myPassTest";

        given(dataSource.getPassword(playerName)).willReturn(password);
        given(method.comparePassword(clearTextPass, password, playerName)).willReturn(true);

        // when
        boolean result = passwordSecurity.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(true));
        verify(dataSource).getPassword(playerName);
        verify(pluginManager).callEvent(any(PasswordEncryptionEvent.class));
        verify(method).comparePassword(clearTextPass, password, playerName);
    }

    @Test
    public void shouldReturnPasswordMismatch() {
        // given
        HashedPassword password = new HashedPassword("$TEST$10$SOME_HASH", null);
        String playerName = "My_PLayer";
        String clearTextPass = "passw0Rd1";

        given(dataSource.getPassword(playerName)).willReturn(password);
        given(method.comparePassword(clearTextPass, password, playerName)).willReturn(false);

        // when
        boolean result = passwordSecurity.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource).getPassword(playerName);
        verify(pluginManager).callEvent(any(PasswordEncryptionEvent.class));
        verify(method).comparePassword(clearTextPass, password, playerName);
    }

    @Test
    public void shouldReturnFalseIfPlayerDoesNotExist() {
        // given
        String playerName = "bobby";
        String clearTextPass = "tables";
        given(dataSource.getPassword(playerName)).willReturn(null);

        // when
        boolean result = passwordSecurity.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource).getPassword(playerName);
        verify(method, never()).comparePassword(anyString(), any(HashedPassword.class), anyString());
    }

    @Test
    public void shouldTryOtherMethodsForFailedPassword() {
        // given
        // BCRYPT hash for "Test"
        HashedPassword password =
            new HashedPassword("$2y$10$2e6d2193f43501c926e25elvWlPmWczmrfrnbZV0dUZGITjYjnkkW");
        String playerName = "somePlayer";
        String clearTextPass = "Test";
        // MD5 hash for "Test"
        HashedPassword newPassword = new HashedPassword("0cbc6611f5540bd0809a388dc95a615b");

        given(dataSource.getPassword(argThat(equalToIgnoringCase(playerName)))).willReturn(password);
        given(method.comparePassword(clearTextPass, password, playerName)).willReturn(false);
        given(method.computeHash(clearTextPass, playerName)).willReturn(newPassword);
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.MD5);
        given(settings.getProperty(SecuritySettings.LEGACY_HASHES)).willReturn(newHashSet(HashAlgorithm.BCRYPT));
        passwordSecurity.reload();

        // when
        boolean result = passwordSecurity.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(true));
        // Note ljacqu 20151230: We need to check the player name in a case-insensitive way because the methods within
        // PasswordSecurity may convert the name into all lower-case. This is desired because EncryptionMethod methods
        // should only be invoked with all lower-case names. Data source is case-insensitive itself, so this is fine.
        verify(dataSource).getPassword(argThat(equalToIgnoringCase(playerName)));
        verify(pluginManager, times(2)).callEvent(any(PasswordEncryptionEvent.class));
        verify(method).comparePassword(clearTextPass, password, playerName);
        verify(dataSource).updatePassword(playerName, newPassword);
    }

    @Test
    public void shouldTryLegacyMethodsAndFail() {
        // given
        HashedPassword password = new HashedPassword("hashNotMatchingAnyMethod", "someBogusSalt");
        String playerName = "asfd";
        String clearTextPass = "someInvalidPassword";
        given(dataSource.getPassword(playerName)).willReturn(password);
        given(method.comparePassword(clearTextPass, password, playerName)).willReturn(false);
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.MD5);
        given(settings.getProperty(SecuritySettings.LEGACY_HASHES)).willReturn(
            newHashSet(HashAlgorithm.DOUBLEMD5, HashAlgorithm.JOOMLA, HashAlgorithm.SMF, HashAlgorithm.SHA256));
        passwordSecurity.reload();

        // when
        boolean result = passwordSecurity.comparePassword(clearTextPass, playerName);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource, never()).updatePassword(anyString(), any(HashedPassword.class));
    }

    @Test
    public void shouldHashPassword() {
        // given
        String password = "MyP@ssword";
        String username = "theUserInTest";
        String usernameLowerCase = username.toLowerCase(Locale.ROOT);
        HashedPassword hashedPassword = new HashedPassword("$T$est#Hash", "__someSalt__");
        given(method.computeHash(password, usernameLowerCase)).willReturn(hashedPassword);
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.JOOMLA);
        passwordSecurity.reload();

        // when
        HashedPassword result = passwordSecurity.computeHash(password, username);

        // then
        assertThat(result, equalTo(hashedPassword));
        // Check that an event was fired twice: once on test setup, and once because we called reload()
        verify(pluginManager, times(2)).callEvent(any(PasswordEncryptionEvent.class));
        assertThat(Joomla.class.equals(caughtClassInEvent), equalTo(true));
    }

    @Test
    public void shouldSkipCheckIfMandatorySaltIsUnavailable() {
        // given
        String password = "?topSecretPass\\";
        String username = "someone12";
        HashedPassword hashedPassword = new HashedPassword("~T!est#Hash");
        given(method.hasSeparateSalt()).willReturn(true);
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.XAUTH);
        passwordSecurity.reload();

        // when
        boolean result = passwordSecurity.comparePassword(password, hashedPassword, username);

        // then
        assertThat(result, equalTo(false));
        verify(dataSource, never()).getAuth(anyString());
        // Check that an event was fired twice: once on test setup, and once because we called reload()
        verify(pluginManager, times(2)).callEvent(any(PasswordEncryptionEvent.class));
        verify(method, never()).comparePassword(anyString(), any(HashedPassword.class), anyString());
    }

    @Test
    public void shouldReloadSettings() {
        // given
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.MD5);
        given(settings.getProperty(SecuritySettings.LEGACY_HASHES))
            .willReturn(newHashSet(HashAlgorithm.CUSTOM, HashAlgorithm.BCRYPT));
        reset(pluginManager); // reset behavior when the event is emitted to check that we create an instance of Md5.java

        // when
        passwordSecurity.reload();

        // then
        assertThat(ReflectionTestUtils.getFieldValue(PasswordSecurity.class, passwordSecurity, "encryptionMethod"),
            instanceOf(Md5.class));
        Set<HashAlgorithm> legacyHashesSet = newHashSet(HashAlgorithm.CUSTOM, HashAlgorithm.BCRYPT);
        assertThat(ReflectionTestUtils.getFieldValue(PasswordSecurity.class, passwordSecurity, "legacyAlgorithms"),
            equalTo(legacyHashesSet));
    }
}
