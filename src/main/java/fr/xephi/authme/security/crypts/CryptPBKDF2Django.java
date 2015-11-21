package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import fr.xephi.authme.security.pbkdf2.PBKDF2Engine;
import fr.xephi.authme.security.pbkdf2.PBKDF2Parameters;

/**
 */
public class CryptPBKDF2Django implements EncryptionMethod {

    /**
     * Method getHash.
     * @param password String
     * @param salt String
     * @param name String
     * @return String
     * @throws NoSuchAlgorithmException
     * @see fr.xephi.authme.security.crypts.EncryptionMethod#getHash(String, String, String)
     */
    @Override
    public String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException {
        String result = "pbkdf2_sha256$15000$" + salt + "$";
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), 15000);
        PBKDF2Engine engine = new PBKDF2Engine(params);

        return result + String.valueOf(DatatypeConverter.printBase64Binary(engine.deriveKey(password, 32)));
    }

    /**
     * Method comparePassword.
     * @param hash String
     * @param password String
     * @param playerName String
     * @return boolean
     * @throws NoSuchAlgorithmException
     * @see fr.xephi.authme.security.crypts.EncryptionMethod#comparePassword(String, String, String)
     */
    @Override
    public boolean comparePassword(String hash, String password,
                                   String playerName) throws NoSuchAlgorithmException {
        String[] line = hash.split("\\$");
        String salt = line[2];
        byte[] derivedKey = DatatypeConverter.parseBase64Binary(line[3]);
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), 15000, derivedKey);
        PBKDF2Engine engine = new PBKDF2Engine(params);
        return engine.verifyKey(password);
    }

}
