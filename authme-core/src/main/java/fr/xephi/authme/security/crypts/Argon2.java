package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Recommendation(Usage.RECOMMENDED)
@HasSalt(value = SaltType.TEXT, length = 16)
// Note: Argon2 is actually a salted algorithm but salt generation is handled internally
// and isn't exposed to the outside, so we treat it as an unsalted implementation
public class Argon2 extends UnsaltedMethod {

    private static final int ITERATIONS = 2;
    private static final int MEMORY_KB = 65536;
    private static final int PARALLELISM = 1;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;

    private final SecureRandom random = new SecureRandom();

    @Override
    public String computeHash(String password) {
        byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        byte[] hash = derive(password.toCharArray(), salt, ITERATIONS, MEMORY_KB, PARALLELISM, HASH_BYTES);
        Base64.Encoder enc = Base64.getEncoder().withoutPadding();
        return "$argon2i$v=19$m=" + MEMORY_KB + ",t=" + ITERATIONS + ",p=" + PARALLELISM
            + "$" + enc.encodeToString(salt)
            + "$" + enc.encodeToString(hash);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        String[] parts = hashedPassword.getHash().split("\\$");
        // Expected: ["", "argon2i", "v=19", "m=65536,t=2,p=1", "<salt_b64>", "<hash_b64>"]
        if (parts.length != 6 || !"argon2i".equals(parts[1])) {
            return false;
        }
        try {
            int[] params = parseParams(parts[3]); // m, t, p
            byte[] salt = decodeNoPadding(parts[4]);
            byte[] expected = decodeNoPadding(parts[5]);
            byte[] computed = derive(password.toCharArray(), salt, params[1], params[0], params[2], expected.length);
            return MessageDigest.isEqual(computed, expected);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private static byte[] derive(char[] password, byte[] salt, int iterations, int memoryKb, int parallelism, int hashLen) {
        Argon2Parameters params = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_i)
            .withVersion(Argon2Parameters.ARGON2_VERSION_13)
            .withIterations(iterations)
            .withMemoryAsKB(memoryKb)
            .withParallelism(parallelism)
            .withSalt(salt)
            .build();
        Argon2BytesGenerator generator = new Argon2BytesGenerator();
        generator.init(params);
        byte[] result = new byte[hashLen];
        generator.generateBytes(password, result);
        return result;
    }

    /** Parses "m=65536,t=2,p=1" → [m, t, p]. */
    private static int[] parseParams(String paramStr) {
        int[] result = new int[3];
        for (String kv : paramStr.split(",")) {
            String[] pair = kv.split("=");
            int v = Integer.parseInt(pair[1]);
            switch (pair[0]) {
                case "m": result[0] = v; break;
                case "t": result[1] = v; break;
                case "p": result[2] = v; break;
            }
        }
        return result;
    }

    /** Decodes base64 without padding (PHC format omits '='). */
    private static byte[] decodeNoPadding(String s) {
        switch (s.length() % 4) {
            case 2: s += "=="; break;
            case 3: s += "="; break;
            default: break;
        }
        return Base64.getDecoder().decode(s);
    }
}
