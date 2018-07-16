package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.HashUtils.isEqual;

@Recommendation(Usage.ACCEPTABLE)
public class Joomla extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return HashUtils.md5(password + salt) + ":" + salt;
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String unusedName) {
        String hash = hashedPassword.getHash();
        String[] hashParts = hash.split(":");
        return hashParts.length == 2 && isEqual(hash, computeHash(password, hashParts[1], null));
    }

    @Override
    public int getSaltLength() {
        return 32;
    }

}
