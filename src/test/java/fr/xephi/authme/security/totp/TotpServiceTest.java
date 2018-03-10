package fr.xephi.authme.security.totp;

import fr.xephi.authme.data.auth.PlayerAuth;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link TotpService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TotpServiceTest {

    @InjectMocks
    private TotpService totpService;

    @Mock
    private TotpAuthenticator totpAuthenticator;

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
        boolean result = totpService.verifyCode(auth, inputCode);

        // then
        assertThat(result, equalTo(true));
        verify(totpAuthenticator).checkCode(totpKey, inputCode);
    }

}
