package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

/**
 */
public class WBB4 implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return BCRYPT.getDoubleHash(password, salt);
    }

    @Override
    public boolean comparePassword(String hash, String password, String playerName) {
        return BCRYPT.checkpw(password, hash, 2);
    }

}
