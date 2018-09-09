package fr.xephi.authme.security.crypts;

import at.favre.lib.crypto.bcrypt.BCrypt;
import at.favre.lib.crypto.bcrypt.IllegalBCryptFormatException;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.util.RandomStringUtils;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Implementation for Ipb4 (Invision Power Board 4).
 * <p>
 * The hash uses standard BCrypt with 13 as log<sub>2</sub> number of rounds. Additionally,
 * Ipb4 requires that the salt be stored in the column "members_pass_hash" as well
 * (even though BCrypt hashes already contain the salt within themselves).
 */
@Recommendation(Usage.DOES_NOT_WORK)
@HasSalt(value = SaltType.TEXT, length = BCryptHasher.SALT_LENGTH_ENCODED)
public class Ipb4 implements EncryptionMethod {

    private BCryptHasher bCryptHasher = new BCryptHasher(BCrypt.Version.VERSION_2A, 13);

    @Override
    public String computeHash(String password, String salt, String name) {
        // Since the radix64-encoded salt is necessary to be stored separately as well, the incoming salt here is
        // radix64-encoded (see #generateSalt()). This means we first need to decode it before passing into the
        // bcrypt hasher... We cheat by inserting the encoded salt into a dummy bcrypt hash so that we can parse it
        // with the BCrypt utilities.
        // This method (with specific salt) is only used for testing purposes, so this approach should be OK.

        String dummyHash = "$2a$10$" + salt + "3Cfb5GnwvKhJ20r.hMjmcNkIT9.Uh9K";
        try {
            BCrypt.HashData parseResult = BCrypt.Version.VERSION_2A.parser.parse(dummyHash.getBytes(UTF_8));
            return bCryptHasher.hashWithRawSalt(password, parseResult.rawSalt);
        } catch (IllegalBCryptFormatException |IllegalArgumentException e) {
            throw new IllegalStateException("Cannot parse hash with salt '" + salt + "'", e);
        }
    }

    @Override
    public HashedPassword computeHash(String password, String name) {
        HashedPassword hash = bCryptHasher.hash(password);

        // 7 chars prefix, then 22 chars which is the encoded salt, which we need again
        String salt = hash.getHash().substring(7, 29);
        return new HashedPassword(hash.getHash(), salt);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        return BCryptHasher.comparePassword(password, hashedPassword.getHash());
    }

    @Override
    public String generateSalt() {
        return RandomStringUtils.generateLowerUpper(22);
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }
}
