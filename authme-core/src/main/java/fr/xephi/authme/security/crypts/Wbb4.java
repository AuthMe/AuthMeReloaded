package fr.xephi.authme.security.crypts;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static fr.xephi.authme.security.HashUtils.isEqual;
import static fr.xephi.authme.security.crypts.BCryptHasher.BYTES_IN_SALT;
import static fr.xephi.authme.security.crypts.BCryptHasher.SALT_LENGTH_ENCODED;

@Recommendation(Usage.RECOMMENDED)
@HasSalt(value = SaltType.TEXT, length = SALT_LENGTH_ENCODED)
public class Wbb4 implements EncryptionMethod {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(Wbb4.class);
    private BCryptHasher bCryptHasher = new BCryptHasher("2a", 8);
    private SecureRandom random = new SecureRandom();

    @Override
    public HashedPassword computeHash(String password, String name) {
        byte[] salt = new byte[BYTES_IN_SALT];
        random.nextBytes(salt);

        String hash = hashInternal(password, salt);
        return new HashedPassword(hash);
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return hashInternal(password, salt.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        String hash = hashedPassword.getHash();
        // BCrypt hash format: $2a$<2-digit-cost>$<22-char-salt><31-char-hash>
        // Salt starts at index 7 ($2a$08$ = 7 chars), is 22 chars long
        if (hash.length() < 29) {
            return false;
        }
        try {
            byte[] salt = BCryptHasher.decodeSalt(hash.substring(7, 29));
            String computedHash = hashInternal(password, salt);
            return isEqual(hash, computedHash);
        } catch (Exception e) {
            logger.logException("Invalid WBB4 hash:", e);
        }
        return false;
    }

    /**
     * Hashes the given password with the provided salt twice: hash(hash(password, salt), salt).
     *
     * @param password the password to hash
     * @param rawSalt the salt to use
     * @return WBB4-compatible hash
     */
    private String hashInternal(String password, byte[] rawSalt) {
        return bCryptHasher.hashWithRawSalt(bCryptHasher.hashWithRawSalt(password, rawSalt), rawSalt);
    }

    @Override
    public String generateSalt() {
        return BCryptHasher.generateSalt();
    }

    @Override
    public boolean hasSeparateSalt() {
        return false;
    }
}
