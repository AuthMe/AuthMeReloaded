package fr.xephi.authme.cache.auth;

/**
 * Stored data for email recovery.
 */
public class EmailRecoveryData {

    private final String email;
    private final String recoveryCode;

    /**
     * Constructor.
     *
     * @param email the email address
     * @param recoveryCode the recovery code, or null if not available
     * @param codeExpiration expiration timestamp of the recovery code
     */
    public EmailRecoveryData(String email, String recoveryCode, Long codeExpiration) {
        this.email = email;

        if (codeExpiration == null || System.currentTimeMillis() > codeExpiration) {
            this.recoveryCode = null;
        } else {
            this.recoveryCode = recoveryCode;
        }
    }

    /**
     * @return the email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * @return the recovery code, if available and not expired
     */
    public String getRecoveryCode() {
        return recoveryCode;
    }
}
