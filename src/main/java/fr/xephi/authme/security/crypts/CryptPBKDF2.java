package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.security.pbkdf2.PBKDF2Engine;
import fr.xephi.authme.security.pbkdf2.PBKDF2Parameters;

import java.util.Arrays;

@Recommendation(Usage.DOES_NOT_WORK)
public class CryptPBKDF2 extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        String result = "pbkdf2_sha256$10000$" + salt + "$";
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), 10000);
        PBKDF2Engine engine = new PBKDF2Engine(params);

        return result + Arrays.toString(engine.deriveKey(password, 64));
    }

    @Override
    public boolean comparePassword(String password, EncryptedPassword encryptedPassword, String unusedName) {
        String[] line = encryptedPassword.getHash().split("\\$");
        String salt = line[2];
        String derivedKey = line[3];
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), 10000, derivedKey.getBytes());
        PBKDF2Engine engine = new PBKDF2Engine(params);
        return engine.verifyKey(password);
    }

    @Override
    public int getSaltLength() {
        return 12;
    }

}
