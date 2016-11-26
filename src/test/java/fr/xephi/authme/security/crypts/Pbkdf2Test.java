package fr.xephi.authme.security.crypts;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link Pbkdf2}.
 */
public class Pbkdf2Test extends AbstractEncryptionMethodTest {

    public Pbkdf2Test() {
        super(new Pbkdf2(mockSettings()),
            "pbkdf2_sha256$10000$b25801311edf$093E38B16DFF13FCE5CD64D5D888EE6E0376A3E572FE5DA6749515EA0F384413223A21C464B0BE899E64084D1FFEFD44F2AC768453C87F41B42CC6954C416900",  // password
            "pbkdf2_sha256$10000$fe705da06c57$A41527BD58FED9C9E6F452FC1BA8B0C4C4224ECC63E37F71EB1A0865D2AB81BBFEBCA9B7B6A6E8AEF4717B43F8EB6FB4EDEFFBB399D9D991EF7E23013595BAF0",  // PassWord1
            "pbkdf2_sha256$10000$05603593cdda$1D30D1D90D826C866755969F06C312E21CC3E8DA0B777E2C764700E4E1FD890B731FAF44753D68F3FC025D3EAA709E800FBF2AF61DB23464311FCE7D35353A30",  // &^%te$t?Pw@_
            "pbkdf2_sha256$10000$fb944d66d754$F7E3BF8CB07CE3B3C8C5C534F803252F7B4FD58832E33BA62BA46CA06F23BAE12BE03A9CB5874BCFD4469E42972406F920E59F002247B23C22A8CF3D0E7BFFE0"); // âË_3(íù*
    }

    @Test
    public void shouldDetectMatchForHashWithOtherRoundNumber() {
        // given
        Pbkdf2 pbkdf2 = new Pbkdf2(mockSettings());
        String hash = "pbkdf2_sha256$4128$3469b0d48b702046$DC8A54351008C6054E12FB19E0BF8A4EA6D4165E0EDC97A1ECD15231037C382DE5BF85D07D5BC9D1ADF9BBFE4CE257C6059FB1B9FF65DB69D8B205F064BE0DA9";
        String clearText = "PassWord1";

        // when
        boolean isMatch = pbkdf2.comparePassword(clearText, new HashedPassword(hash), "");

        // then
        assertThat(isMatch, equalTo(true));
    }

    private static Settings mockSettings() {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.PBKDF2_NUMBER_OF_ROUNDS)).willReturn(4128);
        return settings;
    }
}
