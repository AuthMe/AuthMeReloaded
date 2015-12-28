package fr.xephi.authme.security.crypts;

/**
 * Temporary interface for additional methods that will be added to {@link EncryptionMethod}.
 * TODO #358: Move methods to EncryptionMethod interface and delete this.
 */
public interface NewEncrMethod extends EncryptionMethod {

    HashResult computeHash(String password, String name);

    String generateSalt();

    boolean hasSeparateSalt();

    boolean comparePassword(String hash, String password, String salt, String name);

}
