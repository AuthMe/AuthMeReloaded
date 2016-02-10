package fr.xephi.authme.security.crypts.description;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.crypts.BCryptService;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.StringUtils;

import java.security.SecureRandom;


@Recommendation(Usage.DOES_NOT_WORK)
@HasSalt(value = SaltType.TEXT)
public class IPB4 implements EncryptionMethod {
    private SecureRandom random = new SecureRandom();

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
            return hash.getHash().length() > 3 && BCryptService.checkpw(password, hash.getHash());
        } catch (IllegalArgumentException e) {
            ConsoleLogger.showError("Bcrypt checkpw() returned " + StringUtils.formatException(e));
        }
        return false;
    }

    @Override
    public String generateSalt() {
        StringBuilder sb = new StringBuilder(22);
        for (int i = 0; i < 22; i++) {
            char chr;
            do {
                chr = (char) (random.nextInt((122 - 48) + 1) + 48);
            }
            while ((chr >= 58 && chr <= 64) || (chr >= 91 && chr <= 96));
            sb.append(chr);
        }
        return sb.toString();
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }
}
