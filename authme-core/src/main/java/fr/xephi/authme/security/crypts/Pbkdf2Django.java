package fr.xephi.authme.security.crypts;

import com.google.common.primitives.Ints;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.description.AsciiRestricted;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.KeyParameter;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@AsciiRestricted
public class Pbkdf2Django extends HexSaltedMethod {

    private static final int DEFAULT_ITERATIONS = 24000;
    private static final int HASH_BYTES = 32;
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(Pbkdf2Django.class);

    @Override
    public String computeHash(String password, String salt, String name) {
        byte[] derived = derive(password, salt.getBytes(StandardCharsets.US_ASCII), DEFAULT_ITERATIONS);
        return "pbkdf2_sha256$" + DEFAULT_ITERATIONS + "$" + salt + "$"
            + Base64.getEncoder().encodeToString(derived);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String unusedName) {
        String[] line = hashedPassword.getHash().split("\\$");
        if (line.length != 4) {
            return false;
        }
        Integer iterations = Ints.tryParse(line[1]);
        if (iterations == null) {
            logger.warning("Cannot read number of rounds for Pbkdf2Django: '" + line[1] + "'");
            return false;
        }
        String salt = line[2];
        byte[] expected = Base64.getDecoder().decode(line[3]);
        byte[] computed = derive(password, salt.getBytes(StandardCharsets.US_ASCII), iterations);
        return MessageDigest.isEqual(computed, expected);
    }

    @Override
    public int getSaltLength() {
        return 12;
    }

    private static byte[] derive(String password, byte[] saltBytes, int iterations) {
        PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
        gen.init(password.getBytes(StandardCharsets.US_ASCII), saltBytes, iterations);
        return ((KeyParameter) gen.generateDerivedMacParameters(HASH_BYTES * 8)).getKey();
    }
}
