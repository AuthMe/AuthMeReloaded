package fr.xephi.authme.security.crypts;

import fr.xephi.authme.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link TwoFactor}.
 */
public class TwoFactorTest {

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldGenerateBarcodeUrl() {
        // given
        String user = "tester";
        String host = "192.168.0.4";
        String secret = "3AK6Y4KWGRLJMEQW";

        // when
        String url = TwoFactor.getQrBarcodeUrl(user, host, secret);

        // then
        String expected = "https://www.google.com/chart?chs=130x130&chld=M%7C0&cht=qr"
            + "&chl=otpauth://totp/tester@192.168.0.4%3Fsecret%3D3AK6Y4KWGRLJMEQW";
        assertThat(url, equalTo(expected));
    }

    @Test
    public void shouldHandleInvalidHash() {
        // given
        HashedPassword password = new HashedPassword("!@&#@!(*&@");
        String inputPassword = "12345";
        TwoFactor twoFactor = new TwoFactor();

        // when
        boolean result = twoFactor.comparePassword(inputPassword, password, "name");

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldHandleInvalidInput() {
        // given
        HashedPassword password = new HashedPassword("3AK6Y4KWGRLJMEQW");
        String inputPassword = "notA_number!";
        TwoFactor twoFactor = new TwoFactor();

        // when
        boolean result = twoFactor.comparePassword(inputPassword, password, "name");

        // then
        assertThat(result, equalTo(false));
    }
}
