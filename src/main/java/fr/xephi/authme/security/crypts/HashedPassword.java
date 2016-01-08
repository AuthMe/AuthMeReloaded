package fr.xephi.authme.security.crypts;

/**
 * The result of a hash computation. See {@link #salt} for details.
 */
public class HashedPassword {

    /** The generated hash. */
    private final String hash;
    /**
     * The generated salt; may be null if no salt is used or if the salt is included
     * in the hash output. The salt is only not null if {@link EncryptionMethod#hasSeparateSalt()}
     * returns true for the associated encryption method.
     * <p>
     * When the field is not null, it must be stored into the salt column of the data source
     * and retrieved again to compare a password with the hash.
     */
    private final String salt;

    /**
     * Constructor.
     *
     * @param hash The computed hash
     * @param salt The generated salt
     */
    public HashedPassword(String hash, String salt) {
        this.hash = hash;
        this.salt = salt;
    }

    /**
     * Constructor for a hash with no separate salt.
     *
     * @param hash The computed hash
     */
    public HashedPassword(String hash) {
        this(hash, null);
    }

    public String getHash() {
        return hash;
    }

    public String getSalt() {
        return salt;
    }

}
