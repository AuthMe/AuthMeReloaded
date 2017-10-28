package fr.xephi.authme.security.crypts;

import de.mkammerer.argon2.Argon2Constants;
import de.mkammerer.argon2.Argon2Factory;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.RECOMMENDED)
@HasSalt(value = SaltType.TEXT, length = Argon2Constants.DEFAULT_SALT_LENGTH)
// Note: Argon2 is actually a salted algorithm but salt generation is handled internally
// and isn't exposed to the outside, so we treat it as an unsalted implementation
public class Argon2 extends UnsaltedMethod {

    private de.mkammerer.argon2.Argon2 argon2;

    public Argon2() {
        argon2 = Argon2Factory.create();
    }

    /**
     * Checks if the argon2 library is available in java.library.path.
     *
     * @return true if the library is present, false otherwise
     */
    public static boolean isLibraryLoaded() {
        try {
            System.loadLibrary("argon2");
            return true;
        } catch (UnsatisfiedLinkError e) {
            ConsoleLogger.logException(
                "Cannot find argon2 library: https://github.com/AuthMe/AuthMeReloaded/wiki/Argon2-as-Password-Hash", e);
        }
        return false;
    }

    @Override
    public String computeHash(String password) {
        return argon2.hash(2, 65536, 1, password);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        return argon2.verify(hashedPassword.getHash(), password);
    }
}
