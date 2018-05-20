package fr.xephi.authme.security;

/**
 * Contains the results of a password check.
 */
public final class PasswordCheckResult {

    private final boolean isSuccessful;
    private final HashAlgorithm legacyHash;

    private PasswordCheckResult(boolean isSuccessful, HashAlgorithm legacyHash) {
        this.isSuccessful = isSuccessful;
        this.legacyHash = legacyHash;
    }

    public static PasswordCheckResult successful() {
        return new PasswordCheckResult(true, null);
    }

    public static PasswordCheckResult successfulFromLegacyHash(HashAlgorithm legacyHash) {
        return new PasswordCheckResult(true, legacyHash);
    }

    public static PasswordCheckResult failed() {
        return new PasswordCheckResult(false, null);
    }

    /**
     * Returns the hash algorithm for which the password was evaluated successfully. This is only not null
     * if the password returned successfully not for the actively configured hash, but for a legacy hash.
     * Note that matches with most legacy hashes will trigger a migration to the active hash.
     *
     * @return legacy hash matching the password, or null if not applicable
     */
    public HashAlgorithm getLegacyHash() {
        return legacyHash;
    }

    /**
     * @return true if the password was correct, false otherwise
     */
    public boolean isSuccessful() {
        return isSuccessful;
    }
}
