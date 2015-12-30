package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.RECOMMENDED)
public class JOOMLA extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return HashUtils.md5(password + salt) + ":" + salt;
    }

    @Override
    public boolean comparePassword(String password, EncryptedPassword encryptedPassword, String unusedName) {
        String hash = encryptedPassword.getHash();
        String[] hashParts = hash.split(":");
        return hashParts.length == 2 && hash.equals(computeHash(password, hashParts[1], null));
    }

    @Override
    public int getSaltLength() {
        return 32;
    }

}
