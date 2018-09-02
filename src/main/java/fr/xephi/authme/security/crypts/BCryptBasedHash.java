package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.crypts.BCryptHasher.SALT_LENGTH_ENCODED;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Abstract parent for BCrypt-based hash algorithms.
 */
@Recommendation(Usage.RECOMMENDED)
@HasSalt(value = SaltType.TEXT, length = SALT_LENGTH_ENCODED)
public abstract class BCryptBasedHash implements EncryptionMethod {

    private final BCryptHasher bCryptHasher;

    public BCryptBasedHash(BCryptHasher bCryptHasher) {
        this.bCryptHasher = bCryptHasher;
    }

    @Override
    public HashedPassword computeHash(String password, String name) {
        return bCryptHasher.hash(password);
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return bCryptHasher.hashWithRawSalt(password, salt.getBytes(UTF_8));
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        return BCryptHasher.comparePassword(password, hashedPassword.getHash());
    }

    @Override
    public String generateSalt() {
        return BCryptHasher.generateSalt();
    }

    @Override
    public boolean hasSeparateSalt() {
        return false;
    }
}
