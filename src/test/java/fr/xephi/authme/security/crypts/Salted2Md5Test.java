package fr.xephi.authme.security.crypts;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link Salted2Md5}.
 */
class Salted2Md5Test extends AbstractEncryptionMethodTest {

    Salted2Md5Test() {
        super(new Salted2Md5(mockSettings()),
            new HashedPassword("9f3d13dc01a6fe61fd669954174399f3", "9b5f5749"),  // password
            new HashedPassword("b28c32f624a4eb161d6adc9acb5bfc5b", "f750ba32"),  // PassWord1
            new HashedPassword("38dcb83cc68424afe3cda012700c2bb1", "eb2c3394"),  // &^%te$t?Pw@_
            new HashedPassword("ad25606eae5b760c8a2469d65578ac39", "04eee598")); // âË_3(íù*)
    }

    private static Settings mockSettings() {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.DOUBLE_MD5_SALT_LENGTH)).willReturn(8);
        return settings;
    }

}
