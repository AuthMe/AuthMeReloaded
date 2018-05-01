package fr.xephi.authme.security.crypts;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.util.ExceptionUtils;
import fr.xephi.authme.util.RandomStringUtils;


/**
 * Implementation for Ipb4 (Invision Power Board 4).
 * <p>
 * The hash uses standard BCrypt with 13 as log<sub>2</sub> number of rounds. Additionally,
 * Ipb4 requires that the salt be stored additionally in the column "members_pass_hash"
 * (even though BCrypt hashes already have the salt in the result).
 */
@Recommendation(Usage.DOES_NOT_WORK)
@HasSalt(value = SaltType.TEXT, length = 22)
public class Ipb4 implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return BCryptService.hashpw(password, "$2a$13$" + salt);
    }

    @Override
    public HashedPassword computeHash(String password, String name) {
        String salt = generateSalt();
        return new HashedPassword(computeHash(password, salt, name), salt);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hash, String name) {
        try {
            return HashUtils.isValidBcryptHash(hash.getHash()) && BCryptService.checkpw(password, hash.getHash());
        } catch (IllegalArgumentException e) {
            ConsoleLogger.warning("Bcrypt checkpw() returned " + ExceptionUtils.formatException(e));
        }
        return false;
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
