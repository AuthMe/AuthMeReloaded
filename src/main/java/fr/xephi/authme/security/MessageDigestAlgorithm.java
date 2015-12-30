package fr.xephi.authme.security;

import java.security.MessageDigest;

/**
 * The Java-supported names to get a {@link MessageDigest} instance with.
 *
 * @see <a href="http://docs.oracle.com/javase/1.5.0/docs/guide/security/CryptoSpec.html#AppA">
 *      Crypto Spec Appendix A: Standard Names</a>
 */
public enum MessageDigestAlgorithm {

    MD5("MD5"),

    SHA1("SHA-1"),

    SHA256("SHA-256"),

    SHA512("SHA-512");

    private final String key;

    MessageDigestAlgorithm(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
