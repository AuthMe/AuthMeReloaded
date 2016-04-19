package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.RECOMMENDED)
public class SALTEDSHA512 extends SeparateSaltMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return HashUtils.sha512(password + salt);
    }

    @Override
    public String generateSalt() {
        return RandomString.generateHex(32);
    }
}
