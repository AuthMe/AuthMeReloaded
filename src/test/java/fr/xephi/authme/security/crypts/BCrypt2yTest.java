package fr.xephi.authme.security.crypts;

import org.junit.Test;

import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link BCrypt2y}.
 */
public class BCrypt2yTest extends AbstractEncryptionMethodTest {

    public BCrypt2yTest() {
        super(new BCrypt2y(),
            "$2y$10$da641e404b982edf1c7c0uTU9BcKzfA2vWKV05q6r.dCvm/93wqVK",  // password
            "$2y$10$e52c48a76f5b86f5da899uiK/HYocyPsfQXESNbP278rIz08LKEP2",  // PassWord1
            "$2y$10$be6f11548dc5fb4088410ONdC0dXnJ04y1RHcJh5fVF3XK5d.qgqK",  // &^%te$t?Pw@_
            "$2y$10$a8097db1fa4423b93f1b2eF6rMAGFkSX178fpROf/OvCFtrDebp6K"); // âË_3(íù*
    }

    @Test
    public void shouldGenerateWith2yPrefixAndCostFactor10() {
        // given
        BCrypt2y bCrypt2y = new BCrypt2y();

        // when
        HashedPassword result = bCrypt2y.computeHash("test", null);

        // then
        assertThat(result.getHash(), startsWith("$2y$10$"));
        assertThat(result.getSalt(), nullValue());
    }
}
