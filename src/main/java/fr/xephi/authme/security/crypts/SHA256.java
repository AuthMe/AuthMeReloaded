package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.HashUtils.sha256;

@Recommendation(Usage.RECOMMENDED)
@HasSalt(value = SaltType.TEXT, length = 16)
public class SHA256 implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return "$SHA$" + salt + "$" + sha256(sha256(password) + salt);
    }

    public String computeHash(String password, String name) {
        return computeHash(password, generateSalt(), name);
    }

    @Override
    public boolean comparePassword(String hash, String password, String playerName) {
        String[] line = hash.split("\\$");
        return line.length == 4 && hash.equals(computeHash(password, line[2], ""));
    }

    public String generateSalt() {
        return RandomString.generateHex(16);
    }

}
