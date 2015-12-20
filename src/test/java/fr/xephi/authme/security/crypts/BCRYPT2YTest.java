package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.PasswordSecurity;
import org.junit.Ignore;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;

/**
 * Test for {@link BCRYPT2Y}.
 */
@Ignore
// TODO #369: Fix hash & add standard test
public class BCRYPT2YTest {

    @Test
    public void shouldCreateHash() throws NoSuchAlgorithmException {
        String salt = PasswordSecurity.createSalt(16); // As defined in PasswordSecurity
        EncryptionMethod method = new BCRYPT2Y();
        System.out.println(method.computeHash("password", salt, "testPlayer"));
    }

}
