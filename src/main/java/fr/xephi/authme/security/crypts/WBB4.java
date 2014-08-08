package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

public class WBB4 implements EncryptionMethod {

    @Override
    public String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException {
        return BCRYPT.getDoubleHash(password, salt);
    }

    @Override
    public boolean comparePassword(String hash, String password,
            String playerName) throws NoSuchAlgorithmException {
        return BCRYPT.checkpw(password, hash, 2);
    }

}
