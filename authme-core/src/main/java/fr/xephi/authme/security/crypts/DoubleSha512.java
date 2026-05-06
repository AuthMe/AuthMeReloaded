package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.util.RandomStringUtils;

/**
 * Double SHA-512 with a separate salt: {@code sha512(sha512(password) + salt)}.
 * <p>
 * Used for migrating accounts from plugins that apply a double SHA-512 pass with a separate hex salt
 * (e.g. LibreLogin's {@code SHA-512} algorithm).
 */
@Recommendation(Usage.ACCEPTABLE)
public class DoubleSha512 extends SeparateSaltMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return HashUtils.sha512(HashUtils.sha512(password) + salt);
    }

    @Override
    public String generateSalt() {
        return RandomStringUtils.generateHex(32);
    }
}
