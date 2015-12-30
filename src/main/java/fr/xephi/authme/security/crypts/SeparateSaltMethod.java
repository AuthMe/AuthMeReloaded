package fr.xephi.authme.security.crypts;

/**
 * Common supertype for encryption methods which store their salt separately from the hash.
 */
public abstract class SeparateSaltMethod implements EncryptionMethod {

    @Override
    public abstract String computeHash(String password, String salt, String name);

    @Override
    public abstract String generateSalt();

    @Override
    public EncryptedPassword computeHash(String password, String name) {
        String salt = generateSalt();
        return new EncryptedPassword(computeHash(password, salt, name), salt);
    }

    @Override
    public boolean comparePassword(String password, EncryptedPassword encryptedPassword, String name) {
        return encryptedPassword.getHash().equals(computeHash(password, encryptedPassword.getSalt(), null));
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }

}
