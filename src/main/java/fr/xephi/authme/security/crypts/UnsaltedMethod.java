package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

/**
 * Common type for encryption methods which do not use any salt whatsoever.
 */
@Recommendation(Usage.DO_NOT_USE)
@HasSalt(SaltType.NONE)
public abstract class UnsaltedMethod implements NewEncrMethod {

    public abstract String computeHash(String password);

    @Override
    public HashResult computeHash(String password, String name) {
        return new HashResult(computeHash(password));
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return computeHash(password);
    }

    @Override
    @Deprecated
    public boolean comparePassword(String hash, String password, String playerName) {
        return false;
    }

    @Override
    public boolean comparePassword(String hash, String password, String salt, String name) {
        return hash.equals(computeHash(password));
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
