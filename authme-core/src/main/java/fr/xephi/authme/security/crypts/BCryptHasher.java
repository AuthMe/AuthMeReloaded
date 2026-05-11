package fr.xephi.authme.security.crypts;

import fr.xephi.authme.util.RandomStringUtils;
import org.bouncycastle.crypto.generators.OpenBSDBCrypt;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Wraps BouncyCastle's {@link OpenBSDBCrypt} and provides methods suitable for use in AuthMe.
 */
public class BCryptHasher {

    /** Number of bytes in a BCrypt salt (not encoded). */
    public static final int BYTES_IN_SALT = 16;
    /** Number of characters of the salt in its radix64-encoded form. */
    public static final int SALT_LENGTH_ENCODED = 22;

    private final String version;
    private final int costFactor;

    /**
     * Constructor.
     *
     * @param version the BCrypt version string ("2a" or "2y")
     * @param costFactor the log2 cost factor to use
     */
    public BCryptHasher(String version, int costFactor) {
        this.version = version;
        this.costFactor = costFactor;
    }

    public HashedPassword hash(String password) {
        byte[] salt = new byte[BYTES_IN_SALT];
        new SecureRandom().nextBytes(salt);
        String hash = OpenBSDBCrypt.generate(version, password.toCharArray(), salt, costFactor);
        return new HashedPassword(hash);
    }

    public String hashWithRawSalt(String password, byte[] rawSalt) {
        return OpenBSDBCrypt.generate(version, password.toCharArray(), rawSalt, costFactor);
    }

    /**
     * Verifies that the given password is correct for the provided BCrypt hash.
     *
     * @param password the password to check with
     * @param hash the hash to check against
     * @return true if the password matches the hash, false otherwise
     */
    public static boolean comparePassword(String password, String hash) {
        if (fr.xephi.authme.security.HashUtils.isValidBcryptHash(hash)) {
            return OpenBSDBCrypt.checkPassword(hash, password.toCharArray());
        }
        return false;
    }

    /**
     * Generates a salt for usage in BCrypt. The returned salt is not yet encoded.
     *
     * @return the salt for a BCrypt hash
     */
    public static String generateSalt() {
        return RandomStringUtils.generateLowerUpper(BYTES_IN_SALT);
    }

    // BCrypt modified-base64 alphabet (OpenBSD variant)
    private static final byte[] B64_INDEX;
    static {
        B64_INDEX = new byte[128];
        Arrays.fill(B64_INDEX, (byte) -1);
        String table = "./ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        for (int i = 0; i < table.length(); i++) {
            B64_INDEX[table.charAt(i)] = (byte) i;
        }
    }

    /**
     * Decodes a BCrypt-modified-base64 encoded salt (22 chars) into raw bytes (16 bytes).
     * The BCrypt alphabet differs from standard base64 and uses a slightly different character set.
     *
     * @param saltB64 the 22-character BCrypt-base64 encoded salt
     * @return 16 raw salt bytes
     */
    public static byte[] decodeSalt(String saltB64) {
        byte[] out = new byte[BYTES_IN_SALT];
        for (int i = 0, j = 0; i < saltB64.length() - 1 && j < BYTES_IN_SALT; ) {
            int c0 = b64Char(saltB64, i++);
            int c1 = b64Char(saltB64, i++);
            out[j++] = (byte) ((c0 << 2) | (c1 >> 4));
            if (j >= BYTES_IN_SALT || i >= saltB64.length()) break;
            int c2 = b64Char(saltB64, i++);
            out[j++] = (byte) (((c1 & 0x0f) << 4) | (c2 >> 2));
            if (j >= BYTES_IN_SALT || i >= saltB64.length()) break;
            int c3 = b64Char(saltB64, i++);
            out[j++] = (byte) (((c2 & 0x03) << 6) | c3);
        }
        return out;
    }

    private static int b64Char(String s, int pos) {
        char c = s.charAt(pos);
        int v = (c < 128) ? B64_INDEX[c] : -1;
        if (v == -1) {
            throw new IllegalArgumentException("Invalid BCrypt base64 character '" + c + "'");
        }
        return v;
    }
}
