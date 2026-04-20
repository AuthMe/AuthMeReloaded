package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.HashUtils.isEqual;

/**
 * Common type for encryption methods which do not use any salt whatsoever.
 */
@Recommendation(Usage.DO_NOT_USE)
@HasSalt(SaltType.NONE)
public abstract class UnsaltedMethod implements EncryptionMethod {

    public abstract String computeHash(String password);

    @Override
    public HashedPassword computeHash(String password, String name) {
        return new HashedPassword(computeHash(password));
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return computeHash(password);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        return isEqual(hashedPassword.getHash(), computeHash(password));
    }

    @Override
    public String generateSalt() {
        return null;
    }

    @Override
    public boolean hasSeparateSalt() {
        return false;
    }
}
