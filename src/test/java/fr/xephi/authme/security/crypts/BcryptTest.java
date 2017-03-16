package fr.xephi.authme.security.crypts;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link BCrypt}.
 */
public class BCryptTest extends AbstractEncryptionMethodTest {

    public BCryptTest() {
        super(new BCrypt(mockSettings()),
            "$2a$10$6iATmYgwJVc3YONhVcZFve3Cfb5GnwvKhJ20r.hMjmcNkIT9.Uh9K", // password
            "$2a$10$LOhUxhEcS0vgDPv/jkXvCurNb7LjP9xUlEolJGk.Uhgikqc6FtIOi", // PassWord1
            "$2a$10$j9da7SGiaakWhzIms9BtwemLUeIhSEphGUQ3XSlvYgpYsGnGCKRBa", // &^%te$t?Pw@_
            "$2a$10$mkmO3SNzQT/SA5fG/8P8PePz/DI/kKpIH8vd1Owf/fQfFu6F0QyWO"  // âË_3(íù*
        );
    }

    private static Settings mockSettings() {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(HooksSettings.BCRYPT_LOG2_ROUND)).willReturn(8);
        return settings;
    }

}
