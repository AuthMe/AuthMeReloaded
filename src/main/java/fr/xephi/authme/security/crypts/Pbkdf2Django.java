package fr.xephi.authme.security.crypts;

import de.rtner.security.auth.spi.PBKDF2Engine;
import de.rtner.security.auth.spi.PBKDF2Parameters;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.crypts.description.AsciiRestricted;

import java.util.Base64;

@AsciiRestricted
public class Pbkdf2Django extends HexSaltedMethod {

    private static final int DEFAULT_ITERATIONS = 24000;

    @Override
    public String computeHash(String password, String salt, String name) {
        String result = "pbkdf2_sha256$" + DEFAULT_ITERATIONS + "$" + salt + "$";
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), DEFAULT_ITERATIONS);
        PBKDF2Engine engine = new PBKDF2Engine(params);

        return result + Base64.getEncoder().encodeToString(engine.deriveKey(password, 32));
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String unusedName) {
        String[] line = hashedPassword.getHash().split("\\$");
        if (line.length != 4) {
            return false;
        }
        int iterations;
        try {
            iterations = Integer.parseInt(line[1]);
        } catch (NumberFormatException e) {
            ConsoleLogger.logException("Could not read number of rounds for Pbkdf2Django:", e);
            return false;
        }
        String salt = line[2];
        byte[] derivedKey = Base64.getDecoder().decode(line[3]);
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "ASCII", salt.getBytes(), iterations, derivedKey);
        PBKDF2Engine engine = new PBKDF2Engine(params);
        return engine.verifyKey(password);
    }

    @Override
    public int getSaltLength() {
        return 12;
    }

}
