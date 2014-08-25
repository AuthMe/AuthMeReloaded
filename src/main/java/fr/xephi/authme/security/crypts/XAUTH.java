package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

public class XAUTH implements EncryptionMethod {

    @Override
    public String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException {
        String hash = getWhirlpool(salt + password).toLowerCase();
        int saltPos = (password.length() >= hash.length() ? hash.length() - 1 : password.length());
        return hash.substring(0, saltPos) + salt + hash.substring(saltPos);
    }

    @Override
    public boolean comparePassword(String hash, String password,
            String playerName) throws NoSuchAlgorithmException {
        int saltPos = (password.length() >= hash.length() ? hash.length() - 1 : password.length());
        String salt = hash.substring(saltPos, saltPos + 12);
        return hash.equals(getHash(password, salt, ""));
    }

    public static String getWhirlpool(String message) {
        WHIRLPOOL w = new WHIRLPOOL();
        byte[] digest = new byte[WHIRLPOOL.DIGESTBYTES];
        w.NESSIEinit();
        w.NESSIEadd(message);
        w.NESSIEfinalize(digest);
        return WHIRLPOOL.display(digest);
    }

}
