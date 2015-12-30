package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.RECOMMENDED)
public class XAUTH extends HexSaltedMethod {

    private static String getWhirlpool(String message) {
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

    @Override
    public boolean comparePassword(String password, EncryptedPassword encryptedPassword, String playerName) {
        String hash = encryptedPassword.getHash();
        int saltPos = (password.length() >= hash.length() ? hash.length() - 1 : password.length());
        String saltFromHash = hash.substring(saltPos, saltPos + 12);
        return hash.equals(computeHash(password, saltFromHash, null));
    }

    @Override
    public int getSaltLength() {
        return 12;
    }

}
