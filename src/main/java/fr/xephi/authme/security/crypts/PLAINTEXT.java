package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

public class PLAINTEXT implements EncryptionMethod {

    @Override
    public String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException {
        return password;
    }

    @Override
    public boolean comparePassword(String hash, String password,
            String playerName) throws NoSuchAlgorithmException {
        return hash.equals(password);
    }

}
