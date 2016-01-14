package fr.xephi.authme.security.crypts;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.util.StringUtils;

@Recommendation(Usage.DOES_NOT_WORK)
public class WBB4 extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return BCRYPT.getDoubleHash(password, salt);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String playerName) {
        try {
            return BCRYPT.checkpw(password, hashedPassword.getHash(), 2);
        } catch (IllegalArgumentException e) {
            ConsoleLogger.showError("WBB4 compare password returned: " + StringUtils.formatException(e));
        }
        return false;
    }

    @Override
    public String generateSalt() {
        return BCRYPT.gensalt(8);
    }

    /**
     * Note that {@link #generateSalt()} is overridden for this class.
     *
     * @return The salt length
     */
    @Override
    public int getSaltLength() {
        return 8;
    }

}
