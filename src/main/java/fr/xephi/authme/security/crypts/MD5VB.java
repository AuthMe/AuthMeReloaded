package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.HashUtils.md5;

@Recommendation(Usage.OK)
@HasSalt(value = SaltType.TEXT, length = 16)
public class MD5VB implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return "$MD5vb$" + salt + "$" + md5(md5(password) + salt);
    }

    public String computeHash(String password, String name) {
        return computeHash(password, generateSalt(), null);
    }

    @Override
    public boolean comparePassword(String hash, String password, String playerName) {
        String[] line = hash.split("\\$");
        return line.length == 4 && hash.equals(computeHash(password, line[2], ""));
    }

    public String generateSalt() {
        return HashUtils.generateSalt(16);
    }

}
