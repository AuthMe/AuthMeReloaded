package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.util.RandomStringUtils;


/**
 * Implementation for Ipb4 (Invision Power Board 4).
 * <p>
 * The hash uses standard BCrypt with 13 as log<sub>2</sub> number of rounds. Additionally,
 * Ipb4 requires that the salt be stored in the column "members_pass_hash" as well
 * (even though BCrypt hashes already contain the salt within themselves).
 */
@Recommendation(Usage.DOES_NOT_WORK)
@HasSalt(value = SaltType.TEXT, length = BCryptHasher.SALT_LENGTH_ENCODED)
public class Ipb4 implements EncryptionMethod {

    private BCryptHasher bCryptHasher = new BCryptHasher("2a", 13);

    @Override
    public String computeHash(String password, String salt, String name) {
        // The salt here is the 22-char BCrypt-modified-base64 encoded salt (see #generateSalt).
        // This method (with specific salt) is only used for testing purposes.
        try {
            byte[] rawSalt = BCryptHasher.decodeSalt(salt);
            return bCryptHasher.hashWithRawSalt(password, rawSalt);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Cannot parse hash with salt '" + salt + "'", e);
        }
    }

    @Override
    public HashedPassword computeHash(String password, String name) {
        HashedPassword hash = bCryptHasher.hash(password);

        // 7 chars prefix ($2a$XX$), then 22 chars which is the encoded salt, which we need again
        String salt = hash.getHash().substring(7, 29);
        return new HashedPassword(hash.getHash(), salt);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        return BCryptHasher.comparePassword(password, hashedPassword.getHash());
    }

    @Override
    public String generateSalt() {
        return RandomStringUtils.generateLowerUpper(22);
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }
}
