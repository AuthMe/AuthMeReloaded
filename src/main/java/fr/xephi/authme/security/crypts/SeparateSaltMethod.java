package fr.xephi.authme.security.crypts;

/**
 * Common supertype for encryption methods which store their salt separately from the hash.
 */
public abstract class SeparateSaltMethod implements NewEncrMethod {

    @Override
    public abstract String computeHash(String password, String salt, String name);

    @Override
    public abstract String generateSalt();

    @Override
    public HashResult computeHash(String password, String name) {
        String salt = generateSalt();
        return new HashResult(computeHash(password, salt, name), salt);
    }

    @Override
    public boolean comparePassword(String hash, String password, String salt, String name) {
        return hash.equals(computeHash(password, salt, null));
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }

    @Override
    @Deprecated
    public boolean comparePassword(String hash, String password, String playerName) {
        return false;
    }
}
