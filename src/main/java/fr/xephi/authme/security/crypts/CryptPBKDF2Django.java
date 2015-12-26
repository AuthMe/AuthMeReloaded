package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.security.pbkdf2.PBKDF2Engine;
import fr.xephi.authme.security.pbkdf2.PBKDF2Parameters;

import javax.xml.bind.DatatypeConverter;

@Recommendation(Usage.OK)
@HasSalt(value = SaltType.TEXT, length = 12)
public class CryptPBKDF2Django implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        String result = "pbkdf2_sha256$15000$" + salt + "$";
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), 15000);
        PBKDF2Engine engine = new PBKDF2Engine(params);

        return result + String.valueOf(DatatypeConverter.printBase64Binary(engine.deriveKey(password, 32)));
    }

    public String computeHash(String password, String name) {
        return computeHash(password, generateSalt(), null);
    }

    @Override
    public boolean comparePassword(String hash, String password, String playerName) {
        String[] line = hash.split("\\$");
        String salt = line[2];
        byte[] derivedKey = DatatypeConverter.parseBase64Binary(line[3]);
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), 15000, derivedKey);
        PBKDF2Engine engine = new PBKDF2Engine(params);
        return engine.verifyKey(password);
    }

    public String generateSalt() {
        return HashUtils.generateSalt(12);
    }

}
