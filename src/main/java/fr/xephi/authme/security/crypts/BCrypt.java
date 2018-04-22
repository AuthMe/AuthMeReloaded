package fr.xephi.authme.security.crypts;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.util.ExceptionUtils;

import javax.inject.Inject;

@Recommendation(Usage.RECOMMENDED) // provided the salt length is >= 8
@HasSalt(value = SaltType.TEXT) // length depends on the bcryptLog2Rounds setting
public class BCrypt implements EncryptionMethod {

    private final int bCryptLog2Rounds;

    @Inject
    public BCrypt(Settings settings) {
        bCryptLog2Rounds = settings.getProperty(HooksSettings.BCRYPT_LOG2_ROUND);
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return BCryptService.hashpw(password, salt);
    }

    @Override
    public HashedPassword computeHash(String password, String name) {
        String salt = generateSalt();
        return new HashedPassword(BCryptService.hashpw(password, salt), null);
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
        return BCryptService.gensalt(bCryptLog2Rounds);
    }

    @Override
    public boolean hasSeparateSalt() {
        return false;
    }

}
