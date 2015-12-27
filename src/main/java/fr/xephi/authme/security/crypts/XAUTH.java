package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.RECOMMENDED)
@HasSalt(value = SaltType.TEXT, length = 12)
public class XAUTH implements EncryptionMethod {

    public static String getWhirlpool(String message) {
        WHIRLPOOL w = new WHIRLPOOL();
        byte[] digest = new byte[WHIRLPOOL.DIGESTBYTES];
        w.NESSIEinit();
        w.NESSIEadd(message);
        w.NESSIEfinalize(digest);
        return WHIRLPOOL.display(digest);
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        String hash = getWhirlpool(salt + password).toLowerCase();
        int saltPos = (password.length() >= hash.length() ? hash.length() - 1 : password.length());
        return hash.substring(0, saltPos) + salt + hash.substring(saltPos);
    }

    public String computeHash(String password, String name) {
        return computeHash(password, generateSalt(), null);
    }

    @Override
    public boolean comparePassword(String hash, String password, String playerName) {
        int saltPos = (password.length() >= hash.length() ? hash.length() - 1 : password.length());
        String salt = hash.substring(saltPos, saltPos + 12);
        return hash.equals(computeHash(password, salt, ""));
    }

    public String generateSalt() {
        return RandomString.generateHex(12);
    }

}
