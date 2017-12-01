package fr.xephi.authme.security.crypts;

import org.junit.Test;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link Smf}.
 */
public class SmfTest extends AbstractEncryptionMethodTest {

    public SmfTest() {
        super(new Smf(),
            "9b361c66977bb059d460a20d3c21fb3394772df5",  // password
            "31a560bdd095a837945d46add1605108ba87b268",  // PassWord1
            "8d4b84544e0891be8c183fe9b1003cfac18c51a1",  // &^%te$t?Pw@_
            "03cca5af1eb0a93be47777651b2e7be4fd5d537d"); // âË_3(íù*
    }

    @Override
    protected void verifyCorrectConstructorIsUsed(EncryptionMethod method, boolean isSaltConstructor) {
        // Smf declares to use a separate salt, but it's not used in the actual hashing mechanism, see Smf class Javadoc
        assertThat(method.hasSeparateSalt(), equalTo(true));
        assertThat(isSaltConstructor, equalTo(false));
    }

    @Test
    public void shouldGenerateFourCharSalt() {
        // given
        EncryptionMethod method = new Smf();

        // when / then
        assertThat(method.hasSeparateSalt(), equalTo(true));
        for (int i = 0; i < 3; ++i) {
            HashedPassword hash = method.computeHash("pw", "name");
            String salt = hash.getSalt();
            assertThat(salt, stringWithLength(4));
            assertThat(salt.matches("[a-z0-9]{4}"), equalTo(true));
        }
    }
}
