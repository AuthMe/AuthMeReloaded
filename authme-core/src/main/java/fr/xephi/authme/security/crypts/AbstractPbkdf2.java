package fr.xephi.authme.security.crypts;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.KeyParameter;

import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

public abstract class AbstractPbkdf2 extends HexSaltedMethod {

    protected final int numberOfRounds;

    protected AbstractPbkdf2(Settings settings, int defaultRounds) {
        int configured = settings.getProperty(SecuritySettings.PBKDF2_NUMBER_OF_ROUNDS);
        this.numberOfRounds = configured > 0 ? configured : defaultRounds;
    }

    protected byte[] deriveKey(String password, byte[] saltBytes, int iterations, int keyLength) {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(password.getBytes(StandardCharsets.UTF_8), saltBytes, iterations);
        return ((KeyParameter) gen.generateDerivedMacParameters(keyLength * 8)).getKey();
    }

    protected boolean verifyKey(String password, byte[] saltBytes, int iterations, byte[] expectedKey) {
        byte[] computed = deriveKey(password, saltBytes, iterations, expectedKey.length);
        return MessageDigest.isEqual(computed, expectedKey);
    }
}
