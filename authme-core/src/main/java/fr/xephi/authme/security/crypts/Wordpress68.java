package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static fr.xephi.authme.security.crypts.BCryptHasher.SALT_LENGTH_ENCODED;

/**
 * Hash algorithm for WordPress 6.8 and newer.
 * <p>
 * Since WordPress 6.8 passwords are hashed with bcrypt, but the password is pre-hashed with
 * HMAC-SHA384 beforehand in order to work around bcrypt's 72-byte limit. The resulting hash is
 * stored with a {@code $wp} prefix, e.g. {@code $wp$2y$10$...}. This class implements the same
 * scheme so that AuthMe can verify and produce hashes that WordPress understands out of the box.
 * <p>
 * Passwords hashed by WordPress 6.7 and older use phpass ({@code $P$}) and can be verified by
 * configuring {@code WORDPRESS} as a legacy hash. Plain bcrypt hashes ({@code $2y$}) as written
 * by earlier AuthMe versions can be handled by adding {@code BCRYPT2Y} to the legacy hashes.
 */
@Recommendation(Usage.RECOMMENDED)
@HasSalt(value = SaltType.TEXT, length = SALT_LENGTH_ENCODED)
public class Wordpress68 implements EncryptionMethod {

    /** Prefix by which WordPress marks a pre-hashed bcrypt hash. */
    private static final String PREFIX = "$wp";
    /** Key used by WordPress for the HMAC-SHA384 pre-hashing step. */
    private static final String HMAC_KEY = "wp-sha384";
    /** Cost factor used by PHP's password_hash(), on which WordPress relies. */
    private static final int BCRYPT_COST = 10;

    private final BCryptHasher bCryptHasher = new BCryptHasher("2y", BCRYPT_COST);

    @Override
    public HashedPassword computeHash(String password, String name) {
        return new HashedPassword(PREFIX + bCryptHasher.hash(preHash(password)).getHash());
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return PREFIX
            + bCryptHasher.hashWithRawSalt(preHash(password), salt.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        String hash = hashedPassword.getHash();
        if (!hash.startsWith(PREFIX)) {
            return false;
        }
        return BCryptHasher.comparePassword(preHash(password), hash.substring(PREFIX.length()));
    }

    @Override
    public String generateSalt() {
        return BCryptHasher.generateSalt();
    }

    @Override
    public boolean hasSeparateSalt() {
        return false;
    }

    /**
     * Applies WordPress' pre-hashing step: {@code base64(HMAC-SHA384(password, "wp-sha384"))}.
     * Pre-hashing ensures that passwords longer than bcrypt's 72-byte limit are hashed in full.
     *
     * @param password the clear-text password
     * @return the pre-hashed password, to be used as input for bcrypt
     */
    private static String preHash(String password) {
        try {
            Mac mac = Mac.getInstance("HmacSHA384");
            mac.init(new SecretKeySpec(HMAC_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA384"));
            return Base64.getEncoder().encodeToString(mac.doFinal(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new UnsupportedOperationException("HmacSHA384 is not available on this system", e);
        }
    }
}