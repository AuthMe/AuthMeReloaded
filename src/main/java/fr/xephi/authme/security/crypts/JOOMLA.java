package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.OK)
@HasSalt(value = SaltType.TEXT, length = 32)
public class JOOMLA implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return HashUtils.md5(password + salt) + ":" + salt;
    }

    public String computeHash(String password, String name) {
        return computeHash(password, generateSalt(), null);
    }

    public String generateSalt() {
        return HashUtils.generateSalt(32);
    }

    @Override
    public boolean comparePassword(String hash, String password, String playerName) {
        String[] hashParts = hash.split(":");
        if (hashParts.length != 2) {
            return false;
        }
        String salt = hashParts[1];
        return hash.equals(computeHash(password, salt, null));
    }
}
