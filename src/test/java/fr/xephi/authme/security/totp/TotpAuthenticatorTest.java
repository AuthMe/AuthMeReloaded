package fr.xephi.authme.security.totp;

import com.google.common.collect.Table;
import com.warrenstrange.googleauth.IGoogleAuthenticator;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.totp.TotpAuthenticator.TotpGenerationResult;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link TotpAuthenticator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TotpAuthenticatorTest {

    private TotpAuthenticator totpAuthenticator;

    @Mock
    private Settings settings;

    @Mock
    private IGoogleAuthenticator googleAuthenticator;

    @Before
    public void initializeTotpAuthenticator() {
        totpAuthenticator = new TotpAuthenticatorTestImpl(settings);
    }

    @Test
    public void shouldGenerateTotpKey() {
        // given
        // Use the GoogleAuthenticator instance the TotpAuthenticator normally creates to test its parameters
        totpAuthenticator = new TotpAuthenticator(settings);

        Player player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");
        given(settings.getProperty(PluginSettings.SERVER_NAME)).willReturn("MCtopia");

        // when
        TotpGenerationResult key1 = totpAuthenticator.generateTotpKey(player);
        TotpGenerationResult key2 = totpAuthenticator.generateTotpKey(player);

        // then
        assertThat(key1.getTotpKey(), stringWithLength(32));
        assertThat(key2.getTotpKey(), stringWithLength(32));
        assertThat(key1.getAuthenticatorQrCodeUrl(), startsWith("https://api.qrserver.com/v1/create-qr-code/?data="));
        assertThat(key1.getAuthenticatorQrCodeUrl(), containsString("MCtopia"));
        assertThat(key2.getAuthenticatorQrCodeUrl(), startsWith("https://api.qrserver.com/v1/create-qr-code/?data="));
        assertThat(key1.getTotpKey(), not(equalTo(key2.getTotpKey())));
    }

    @Test
    public void shouldCheckCodeAndDeclareItValidOnlyOnce() {
        // given
        String secret = "the_secret";
        int code = 21398;
        given(googleAuthenticator.authorize(secret, code)).willReturn(true);

        // when
        boolean result1 = totpAuthenticator.checkCode("pl", secret, Integer.toString(code));
        boolean result2 = totpAuthenticator.checkCode("pl", secret, Integer.toString(code));

        // then
        assertThat(result1, equalTo(true));
        assertThat(result2, equalTo(false));
        verify(googleAuthenticator).authorize(secret, code);
    }

    @Test
    public void shouldHandleInvalidNumberInput() {
        // given / when
        boolean result = totpAuthenticator.checkCode("foo", "Some_Secret", "123ZZ");

        // then
        assertThat(result, equalTo(false));
        verifyNoInteractions(googleAuthenticator);
    }

    @Test
    public void shouldVerifyCode() {
        // given
        String totpKey = "ASLO43KDF2J";
        PlayerAuth auth = PlayerAuth.builder()
            .name("Maya")
            .totpKey(totpKey)
            .build();
        String inputCode = "408435";
        given(totpAuthenticator.checkCode("Maya", totpKey, inputCode)).willReturn(true);

        // when
        boolean result = totpAuthenticator.checkCode(auth, inputCode);

        // then
        assertThat(result, equalTo(true));
        verify(googleAuthenticator).authorize(totpKey, 408435);
    }

    @Test
    public void shouldRemoveOldEntries() {
        // given
        Table<String, Integer, Long> usedCodes = ReflectionTestUtils.getFieldValue(
            TotpAuthenticator.class, totpAuthenticator, "usedCodes");
        usedCodes.put("bobby", 414213, System.currentTimeMillis());
        usedCodes.put("charlie", 732050, System.currentTimeMillis() - 6 * Utils.MILLIS_PER_MINUTE);
        usedCodes.put("bobby", 236067, System.currentTimeMillis() - 9 * Utils.MILLIS_PER_MINUTE);

        // when
        totpAuthenticator.performCleanup();

        // then
        assertThat(usedCodes.size(), equalTo(1));
        assertThat(usedCodes.contains("bobby", 414213), equalTo(true));
    }

    private final class TotpAuthenticatorTestImpl extends TotpAuthenticator {

        TotpAuthenticatorTestImpl(Settings settings) {
            super(settings);
        }

        @Override
        protected IGoogleAuthenticator createGoogleAuthenticator() {
            return googleAuthenticator;
        }
    }
}
