package fr.xephi.authme.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hashing utilities (interface for common hashing algorithms).
 */
public final class HashUtils {

    private HashUtils() {
    }

    /**
     * Generate the SHA-1 digest of the given message.
     *
     * @param message The message to hash
     * @return The resulting SHA-1 digest
     */
    public static String sha1(String message) {
        return hash(message, MessageDigestAlgorithm.SHA1);
    }

    /**
     * Generate the SHA-256 digest of the given message.
     *
     * @param message The message to hash
     * @return The resulting SHA-256 digest
     */
    public static String sha256(String message) {
        return hash(message, MessageDigestAlgorithm.SHA256);
    }

    /**
     * Generate the SHA-512 digest of the given message.
     *
     * @param message The message to hash
     * @return The resulting SHA-512 digest
     */
    public static String sha512(String message) {
        return hash(message, MessageDigestAlgorithm.SHA512);
    }

    /**
     * Generate the MD5 digest of the given message.
     *
     * @param message The message to hash
     * @return The resulting MD5 digest
     */
    public static String md5(String message) {
        return hash(message, MessageDigestAlgorithm.MD5);
    }

    /**
     * Return a {@link MessageDigest} instance for the given algorithm.
     *
     * @param algorithm The desired algorithm
     * @return MessageDigest instance for the given algorithm
     */
    public static MessageDigest getDigest(MessageDigestAlgorithm algorithm) {
        try {
            return MessageDigest.getInstance(algorithm.getKey());
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Your system seems not to support the hash algorithm '"
                + algorithm.getKey() + "'");
        }
    }

    /**
     * Hash the message with the given algorithm and return the hash in its hexadecimal notation.
     *
     * @param message The message to hash
     * @param algorithm The algorithm to hash the message with
     * @return The digest in its hexadecimal representation
     */
    private static String hash(String message, MessageDigestAlgorithm algorithm) {
        MessageDigest md = getDigest(algorithm);
        md.reset();
        md.update(message.getBytes());
        byte[] digest = md.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }

}
