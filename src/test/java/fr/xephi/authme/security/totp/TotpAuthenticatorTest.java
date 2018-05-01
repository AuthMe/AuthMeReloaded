package fr.xephi.authme.security.totp;

import com.warrenstrange.googleauth.IGoogleAuthenticator;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.totp.TotpAuthenticator.TotpGenerationResult;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link TotpAuthenticator}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TotpAuthenticatorTest {

    private TotpAuthenticator totpAuthenticator;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private IGoogleAuthenticator googleAuthenticator;

    @Before
    public void initializeTotpAuthenticator() {
        totpAuthenticator = new TotpAuthenticatorTestImpl(bukkitService);
    }

    @Test
    public void shouldGenerateTotpKey() {
        // given
        // Use the GoogleAuthenticator instance the TotpAuthenticator normally creates to test its parameters
        totpAuthenticator = new TotpAuthenticator(bukkitService);

        Player player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");
        given(bukkitService.getIp()).willReturn("127.48.44.4");

        // when
        TotpGenerationResult key1 = totpAuthenticator.generateTotpKey(player);
        TotpGenerationResult key2 = totpAuthenticator.generateTotpKey(player);

        // then
        assertThat(key1.getTotpKey(), stringWithLength(16));
        assertThat(key2.getTotpKey(), stringWithLength(16));
        assertThat(key1.getAuthenticatorQrCodeUrl(), startsWith("https://chart.googleapis.com/chart?chs=200x200"));
        assertThat(key2.getAuthenticatorQrCodeUrl(), startsWith("https://chart.googleapis.com/chart?chs=200x200"));
        assertThat(key1.getTotpKey(), not(equalTo(key2.getTotpKey())));
    }

    @Test
    public void shouldCheckCode() {
        // given
        String secret = "the_secret";
        int code = 21398;
        given(googleAuthenticator.authorize(secret, code)).willReturn(true);

        // when
        boolean result = totpAuthenticator.checkCode(secret, Integer.toString(code));

        // then
        assertThat(result, equalTo(true));
        verify(googleAuthenticator).authorize(secret, code);
    }

    @Test
    public void shouldHandleInvalidNumberInput() {
        // given / when
        boolean result = totpAuthenticator.checkCode("Some_Secret", "123ZZ");

        // then
        assertThat(result, equalTo(false));
        verifyZeroInteractions(googleAuthenticator);
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
        given(totpAuthenticator.checkCode(totpKey, inputCode)).willReturn(true);

        // when
        boolean result = totpAuthenticator.checkCode(auth, inputCode);

        // then
        assertThat(result, equalTo(true));
        verify(googleAuthenticator).authorize(totpKey, 408435);
    }

    private final class TotpAuthenticatorTestImpl extends TotpAuthenticator {

        TotpAuthenticatorTestImpl(BukkitService bukkitService) {
            super(bukkitService);
        }

        @Override
        protected IGoogleAuthenticator createGoogleAuthenticator() {
            return googleAuthenticator;
        }
    }
}
