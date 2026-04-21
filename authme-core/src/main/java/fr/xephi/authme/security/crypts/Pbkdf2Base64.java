package fr.xephi.authme.security.crypts;

import com.google.common.primitives.Ints;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.settings.Settings;

import javax.inject.Inject;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * PBKDF2-HmacSHA256 with Base64-encoded salt and hash.
 * Format: {@code pbkdf2$<iterations>$<salt_base64>$<hash_base64>}
 */
@Recommendation(Usage.RECOMMENDED)
public class Pbkdf2Base64 extends AbstractPbkdf2 {

    private static final int DEFAULT_ROUNDS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(Pbkdf2Base64.class);

    @Inject
    Pbkdf2Base64(Settings settings) {
        super(settings, DEFAULT_ROUNDS);
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        byte[] saltBytes = Base64.getDecoder().decode(salt);
        byte[] hash = deriveKey(password, saltBytes, numberOfRounds, HASH_BYTES);
        return "pbkdf2$" + numberOfRounds + "$" + salt + "$" + Base64.getEncoder().encodeToString(hash);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String unusedName) {
        String[] parts = hashedPassword.getHash().split("\\$");
        if (parts.length != 4) {
            return false;
        }
        Integer iterations = Ints.tryParse(parts[1]);
        if (iterations == null) {
            logger.warning("Cannot read number of rounds for Pbkdf2Base64: '" + parts[1] + "'");
            return false;
        }
        byte[] saltBytes = Base64.getDecoder().decode(parts[2]);
        byte[] expectedKey = Base64.getDecoder().decode(parts[3]);
        return verifyKey(password, saltBytes, iterations, expectedKey);
    }

    @Override
    public String generateSalt() {
        byte[] salt = new byte[SALT_BYTES];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    @Override
    public int getSaltLength() {
        return SALT_BYTES;
    }
}
