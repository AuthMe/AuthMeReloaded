package fr.xephi.authme.security.crypts;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;


@Recommendation(Usage.RECOMMENDED) // provided the salt length is >= 8
@HasSalt(value = SaltType.TEXT) // length depends on Settings.bCryptLog2Rounds
public class BCRYPT implements EncryptionMethod {

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
            return hash.getHash().length() > 3 && BCryptService.checkpw(password, hash.getHash());
        } catch (IllegalArgumentException e) {
            ConsoleLogger.showError("Bcrypt checkpw() returned " + StringUtils.formatException(e));
        }
        return false;
    }

    @Override
    public String generateSalt() {
        return BCryptService.gensalt(Settings.bCryptLog2Rounds);
    }

    @Override
    public boolean hasSeparateSalt() {
        return false;
    }
}
