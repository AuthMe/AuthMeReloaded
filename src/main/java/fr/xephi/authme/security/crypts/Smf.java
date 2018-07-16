package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.util.RandomStringUtils;

import static fr.xephi.authme.security.HashUtils.isEqual;

/**
 * Hashing algorithm for SMF forums.
 * <p>
 * The hash algorithm is {@code sha1(strtolower($username) . $password)}. However, an additional four-character
 * salt is generated for each user, used to generate the login cookie. Therefore, this implementation generates a salt
 * and declares that it requires a separate salt (the SMF members table has a not-null constraint on the salt column).
 *
 * @see <a href="https://www.simplemachines.org/">Simple Machines Forum</a>
 */
@Recommendation(Usage.DO_NOT_USE)
@HasSalt(SaltType.USERNAME)
public class Smf implements EncryptionMethod {

    @Override
    public HashedPassword computeHash(String password, String name) {
        return new HashedPassword(computeHash(password, null, name), generateSalt());
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return HashUtils.sha1(name.toLowerCase() + password);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        return isEqual(hashedPassword.getHash(), computeHash(password, null, name));
    }

    @Override
    public String generateSalt() {
        return RandomStringUtils.generate(4);
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }
}
