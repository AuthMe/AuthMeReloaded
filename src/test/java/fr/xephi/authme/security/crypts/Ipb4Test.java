package fr.xephi.authme.security.crypts;

import org.junit.Test;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for {@link Ipb4}.
 */
public class Ipb4Test extends AbstractEncryptionMethodTest {

    public Ipb4Test() {
        super(new Ipb4(),
            new HashedPassword("$2a$13$leEvXu77OIwPwNvtZIJvaeAx8EItGHuR3nIlq8416g0gXeJaQdrr2", "leEvXu77OIwPwNvtZIJval"),  //password
            new HashedPassword("$2a$13$xyTTP9zhQQtRRKIJPv5AuuOGJ6Ni9FLbDhcuIAcPjt3XzCxIWe3Uu", "xyTTP9zhQQtRRKIJPv5Au3"),  //PassWord1
            new HashedPassword("$2a$13$rGBrqErm9DZyzbxIGHlgf.xfA15/4d5Ay/TK.3y9lG3AljcoG9Lsi", "rGBrqErm9DZyzbxIGHlgfN"),  //&^%te$t?Pw@_
            new HashedPassword("$2a$13$18dKXZLoGpeHHL81edM9HuipiUcMjn5VDJHlxwRUMRXfJ1b.ZQrJ.", "18dKXZLoGpeHHL81edM9H6")); //âË_3(íù*
    }

    @Test
    public void shouldCreateHashesWith2aAndCostFactor13() {
        // given
        Ipb4 hashMethod = new Ipb4();

        // when
        HashedPassword result = hashMethod.computeHash("test", "name");

        // then
        assertThat(result.getHash(), startsWith("$2a$13$"));
        assertThat(result.getSalt(), stringWithLength(22));
    }

    @Test
    public void shouldThrowForInvalidSalt() {
        // given
        Ipb4 hashMethod = new Ipb4();

        // when
        try {
            hashMethod.computeHash("pass", "invalid salt", "name");

            // then
            fail("Expected exception to be thrown");
        } catch (IllegalStateException e) {
            assertThat(e.getMessage(), containsString("Cannot parse hash with salt"));
        }
    }
}
