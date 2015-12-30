package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.AsciiRestricted;
import fr.xephi.authme.security.pbkdf2.PBKDF2Engine;
import fr.xephi.authme.security.pbkdf2.PBKDF2Parameters;

import javax.xml.bind.DatatypeConverter;

@AsciiRestricted
public class CryptPBKDF2Django extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        String result = "pbkdf2_sha256$15000$" + salt + "$";
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), 15000);
        PBKDF2Engine engine = new PBKDF2Engine(params);

        return result + String.valueOf(DatatypeConverter.printBase64Binary(engine.deriveKey(password, 32)));
    }

    @Override
    public boolean comparePassword(String password, EncryptedPassword encryptedPassword, String unusedName) {
        String[] line = encryptedPassword.getHash().split("\\$");
        String salt = line[2];
        byte[] derivedKey = DatatypeConverter.parseBase64Binary(line[3]);
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), 15000, derivedKey);
        PBKDF2Engine engine = new PBKDF2Engine(params);
        return engine.verifyKey(password);
    }

    @Override
    public int getSaltLength() {
        return 12;
    }

}
