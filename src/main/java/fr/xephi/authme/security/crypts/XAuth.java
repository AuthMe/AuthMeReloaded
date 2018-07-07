package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.HashUtils.isEqual;

@Recommendation(Usage.RECOMMENDED)
public class XAuth extends HexSaltedMethod {

    private static String getWhirlpool(String message) {
        Whirlpool w = new Whirlpool();
        byte[] digest = new byte[Whirlpool.DIGESTBYTES];
        w.NESSIEinit();
        w.NESSIEadd(message);
        w.NESSIEfinalize(digest);
        return Whirlpool.display(digest);
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        String hash = getWhirlpool(salt + password).toLowerCase();
        int saltPos = password.length() >= hash.length() ? hash.length() - 1 : password.length();
        return hash.substring(0, saltPos) + salt + hash.substring(saltPos);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        String hash = hashedPassword.getHash();
        int saltPos = password.length() >= hash.length() ? hash.length() - 1 : password.length();
        if (saltPos + 12 > hash.length()) {
            return false;
        }
        String salt = hash.substring(saltPos, saltPos + 12);
        return isEqual(hash, computeHash(password, salt, name));
    }

    @Override
    public int getSaltLength() {
        return 12;
    }

}
