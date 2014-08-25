package fr.xephi.authme.security.crypts;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ROYALAUTH implements EncryptionMethod {

    @Override
    public String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException {
        for (int i = 0; i < 25; i++)
            password = hash(password, salt);
        return password;
    }

    public String hash(String password, String salt)
            throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(password.getBytes());
        byte byteData[] = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aByteData : byteData)
            sb.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
        return sb.toString();
    }

    @Override
    public boolean comparePassword(String hash, String password,
            String playerName) throws NoSuchAlgorithmException {
        return hash.equalsIgnoreCase(getHash(password, "", ""));
    }

}
