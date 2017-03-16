package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.HashUtils.sha256;

@Recommendation(Usage.RECOMMENDED)
public class Sha256 extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return "$SHA$" + salt + "$" + sha256(sha256(password) + salt);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String playerName) {
        String hash = hashedPassword.getHash();
        String[] line = hash.split("\\$");
        return line.length == 4 && hash.equals(computeHash(password, line[2], ""));
    }

    @Override
    public int getSaltLength() {
        return 16;
    }

}
