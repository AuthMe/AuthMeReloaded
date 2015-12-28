package fr.xephi.authme.security.crypts;

/**
 * The result of a hash computation.
 */
public class HashResult {
    
    /** The generated hash. */
    private final String hash;
    /** The generated salt; may be null if no salt is used or if the salt is included in the hash output. */
    private final String salt;
    
    public HashResult(String hash, String salt) {
        this.hash = hash;
        this.salt = salt;
    }
    
    public String getHash() {
        return hash;
    }
    
    public String getSalt() {
        return salt;
    }
    
}
