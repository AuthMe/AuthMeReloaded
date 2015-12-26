package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.DO_NOT_USE)
@HasSalt(SaltType.NONE)
public class DOUBLEMD5 implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return computeHash(password, null);
    }

    public String computeHash(String password, String name) {
        return HashUtils.md5(HashUtils.md5(password));
    }

    public String generateSalt() {
        return null;
    }

    @Override
    public boolean comparePassword(String hash, String password, String playerName) {
        return hash.equals(computeHash(password, null, null));
    }

}
