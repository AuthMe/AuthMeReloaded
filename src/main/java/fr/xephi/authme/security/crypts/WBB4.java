package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.DOES_NOT_WORK)
public class WBB4 extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return BCRYPT.getDoubleHash(password, salt);
    }

    @Override
    public boolean comparePassword(String hash, String password, String salt, String playerName) {
        return BCRYPT.checkpw(password, hash, 2);
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
