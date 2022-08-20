package fr.xephi.authme.service;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.security.crypts.Sha256;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Locale;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link MigrationService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class MigrationServiceTest {

    @Mock
    private Settings settings;

    @Mock
    private DataSource dataSource;

    @Mock
    private Sha256 sha256;

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldMigratePlaintextHashes() {
        // given
        PlayerAuth auth1 = authWithNickAndHash("bobby", "test");
        PlayerAuth auth2 = authWithNickAndHash("user", "myPassword");
        PlayerAuth auth3 = authWithNickAndHash("Tester12", "$tester12_pw");
        given(dataSource.getAllAuths()).willReturn(Arrays.asList(auth1, auth2, auth3));
        setSha256MockToUppercase(sha256);
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.PLAINTEXT);

        // when
        MigrationService.changePlainTextToSha256(settings, dataSource, sha256);

        // then
        verify(sha256, times(3)).computeHash(anyString(), anyString());
        verify(dataSource).getAllAuths(); // need to verify this because we use verifyNoMoreInteractions() after
        verify(dataSource).updatePassword(auth1);
        assertThat(auth1.getPassword(), equalToHash("TEST"));
        verify(dataSource).updatePassword(auth2);
        assertThat(auth2.getPassword(), equalToHash("MYPASSWORD"));
        verify(dataSource).updatePassword(auth3);
        assertThat(auth3.getPassword(), equalToHash("$TESTER12_PW"));
        verifyNoMoreInteractions(dataSource);
        verify(settings).setProperty(SecuritySettings.PASSWORD_HASH, HashAlgorithm.SHA256);
    }

    @Test
    public void shouldNotMigrateShaHashes() {
        // given
        PlayerAuth auth1 = authWithNickAndHash("testUser", "abc1234");
        PlayerAuth auth2 = authWithNickAndHash("minecraft", "$SHA$f28930ae09823eba4cd98a3");
        given(dataSource.getAllAuths()).willReturn(Arrays.asList(auth1, auth2));
        setSha256MockToUppercase(sha256);
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.PLAINTEXT);

        // when
        MigrationService.changePlainTextToSha256(settings, dataSource, sha256);

        // then
        verify(sha256).computeHash(eq("abc1234"), argThat(equalToIgnoringCase("testUser")));
        verifyNoMoreInteractions(sha256);
        verify(dataSource).getAllAuths(); // need to verify this because we use verifyNoMoreInteractions() after
        verify(dataSource).updatePassword(auth1);
        assertThat(auth1.getPassword(), equalToHash("ABC1234"));
        verifyNoMoreInteractions(dataSource);
        verify(settings).setProperty(SecuritySettings.PASSWORD_HASH, HashAlgorithm.SHA256);
    }

    @Test
    public void shouldNotMigrateForHashOtherThanPlaintext() {
        // given
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.BCRYPT);

        // when
        MigrationService.changePlainTextToSha256(settings, dataSource, sha256);

        // then
        verify(settings).getProperty(SecuritySettings.PASSWORD_HASH);
        verifyNoMoreInteractions(settings, dataSource, sha256);
    }

    private static PlayerAuth authWithNickAndHash(String nick, String hash) {
        return PlayerAuth.builder()
            .name(nick)
            .password(hash, null)
            .build();
    }

    private static void setSha256MockToUppercase(Sha256 sha256) {
        given(sha256.computeHash(anyString(), anyString())).willAnswer(invocation -> {
            String plainPassword = invocation.getArgument(0);
            return new HashedPassword(plainPassword.toUpperCase(Locale.ROOT), null);
        });
    }
}
