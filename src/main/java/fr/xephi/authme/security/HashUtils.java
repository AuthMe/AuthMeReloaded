package fr.xephi.authme.security;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
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
     * Return whether the given hash starts like a BCrypt hash. Checking with this method
     * beforehand prevents the BcryptService from throwing certain exceptions.
     *
     * @param hash The salt to verify
     * @return True if the salt is valid, false otherwise
     */
    public static boolean isValidBcryptHash(String hash) {
        return hash.length() > 3 && hash.substring(0, 2).equals("$2");
    }

    /**
     * Checks whether the two strings are equal to each other in a time-constant manner.
     * This helps to avoid timing side channel attacks,
     * cf. <a href="https://github.com/AuthMe/AuthMeReloaded/issues/1561">issue #1561</a>.
     *
     * @param string1 first string
     * @param string2 second string
     * @return true if the strings are equal to each other, false otherwise
     */
    public static boolean isEqual(String string1, String string2) {
        return MessageDigest.isEqual(
            string1.getBytes(StandardCharsets.UTF_8), string2.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Hash the message with the given algorithm and return the hash in its hexadecimal notation.
     *
     * @param message The message to hash
     * @param algorithm The algorithm to hash the message with
     * @return The digest in its hexadecimal representation
     */
    public static String hash(String message, MessageDigest algorithm) {
        algorithm.reset();
        algorithm.update(message.getBytes());
        byte[] digest = algorithm.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }

    /**
     * Hash the message with the given algorithm and return the hash in its hexadecimal notation.
     *
     * @param message The message to hash
     * @param algorithm The algorithm to hash the message with
     * @return The digest in its hexadecimal representation
     */
    private static String hash(String message, MessageDigestAlgorithm algorithm) {
        return hash(message, getDigest(algorithm));
    }

}
