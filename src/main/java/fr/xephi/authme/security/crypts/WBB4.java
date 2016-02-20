package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.crypts.BCryptService.hashpw;

@Recommendation(Usage.RECOMMENDED)
public class WBB4 extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return hashpw(hashpw(password, salt), salt);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String playerName) {
        if (hashedPassword.getHash().length() != 60) {
            return false;
        }
        String salt = hashedPassword.getHash().substring(0, 29);
        return computeHash(password, salt, null).equals(hashedPassword.getHash());
    }

    @Override
    public String generateSalt() {
        return BCryptService.gensalt(8);
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
