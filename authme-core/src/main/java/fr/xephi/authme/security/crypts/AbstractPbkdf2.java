package fr.xephi.authme.security.crypts;

import de.rtner.security.auth.spi.PBKDF2Engine;
import de.rtner.security.auth.spi.PBKDF2Parameters;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;

public abstract class AbstractPbkdf2 extends HexSaltedMethod {

    protected final int numberOfRounds;

    protected AbstractPbkdf2(Settings settings, int defaultRounds) {
        int configured = settings.getProperty(SecuritySettings.PBKDF2_NUMBER_OF_ROUNDS);
        this.numberOfRounds = configured > 0 ? configured : defaultRounds;
    }

    protected byte[] deriveKey(String password, byte[] saltBytes, int iterations, int keyLength) {
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "UTF-8", saltBytes, iterations);
        return new PBKDF2Engine(params).deriveKey(password, keyLength);
    }

    protected boolean verifyKey(String password, byte[] saltBytes, int iterations, byte[] expectedKey) {
        PBKDF2Parameters params = new PBKDF2Parameters("HmacSHA256", "UTF-8", saltBytes, iterations, expectedKey);
        return new PBKDF2Engine(params).verifyKey(password);
    }
}
