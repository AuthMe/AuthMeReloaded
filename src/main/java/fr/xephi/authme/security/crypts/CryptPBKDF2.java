package fr.xephi.authme.security.crypts;

import de.rtner.misc.BinTools;
import de.rtner.security.auth.spi.PBKDF2Engine;
import de.rtner.security.auth.spi.PBKDF2Parameters;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.RECOMMENDED)
public class CryptPBKDF2 extends HexSaltedMethod {

    private static final int NUMBER_OF_ITERATIONS = 10_000;

    @Override
    public String computeHash(String password, String salt, String name) {
        String result = "pbkdf2_sha256$" + NUMBER_OF_ITERATIONS + "$" + salt + "$";
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "UTF-8", salt.getBytes(), NUMBER_OF_ITERATIONS);
        PBKDF2Engine engine = new PBKDF2Engine(params);

        return result + BinTools.bin2hex(engine.deriveKey(password, 64));
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String unusedName) {
        String[] line = hashedPassword.getHash().split("\\$");
        if (line.length != 4) {
            return false;
        }
        String salt = line[2];
        byte[] derivedKey = BinTools.hex2bin(line[3]);
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "UTF-8", salt.getBytes(), 10000, derivedKey);
        PBKDF2Engine engine = new PBKDF2Engine(params);
        return engine.verifyKey(password);
    }

    @Override
    public int getSaltLength() {
        return 16;
    }

}
