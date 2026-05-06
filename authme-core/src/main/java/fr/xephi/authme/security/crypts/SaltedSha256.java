package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.util.RandomStringUtils;

/**
 * SHA-256 with a separate salt: {@code sha256(password + salt)}.
 * <p>
 * Used for migrating accounts from plugins that apply a single SHA-256 pass over the concatenation of
 * password and salt (e.g. LibreLogin's {@code LOGIT-SHA-256} algorithm).
 */
@Recommendation(Usage.ACCEPTABLE)
public class SaltedSha256 extends SeparateSaltMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return HashUtils.sha256(password + salt);
    }

    @Override
    public String generateSalt() {
        return RandomStringUtils.generateHex(32);
    }
}
