package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.DOES_NOT_WORK)
public class BCRYPT2Y extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        if (salt.length() == 22)
            salt = "$2y$10$" + salt;
        return BCRYPT.hashpw(password, salt);
    }

    @Override
    public boolean comparePassword(String hash, String password, String salt, String playerName) {
        String ok = hash.substring(0, 29);
        return ok.length() == 29 && hash.equals(computeHash(password, ok, playerName));
    }

    @Override
    public int getSaltLength() {
        return 22;
    }

}
