package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.DO_NOT_USE)
@HasSalt(SaltType.USERNAME)
public class SMF implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return computeHash(password, name);
    }

    public String computeHash(String password, String name) {
        return HashUtils.sha1(name.toLowerCase() + password);
    }

    @Override
    public boolean comparePassword(String hash, String password, String playerName) {
        return hash.equals(computeHash(password, playerName));
    }

    public String generateSalt() {
        return null;
    }
}
