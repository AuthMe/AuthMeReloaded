package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

public class BCRYPT2Y implements EncryptionMethod {

    @Override
    public String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException {
        if (salt.length() == 22)
            salt = "$2y$10$" + salt;
        return (BCRYPT.hashpw(password, salt));
    }

    @Override
    public boolean comparePassword(String hash, String password,
            String playerName) throws NoSuchAlgorithmException {
        String ok = hash.substring(0, 29);
        if (ok.length() != 29)
            return false;
        return hash.equals(getHash(password, ok, playerName));
    }

}
