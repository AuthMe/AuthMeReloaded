package fr.xephi.authme.data.captcha;

import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.expiring.ExpiringMap;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Primitive service for storing captcha codes.
 */
public class CaptchaCodeStorage {

    /** Map of captcha codes (with player name as key, case-insensitive). */
    private ExpiringMap<String, String> captchaCodes;
    /** Number of characters newly generated captcha codes should have. */
    private int captchaLength;

    /**
     * Constructor.
     *
     * @param expirationInMinutes minutes after which a saved captcha code expires
     * @param captchaLength the number of characters a captcha code should have
     */
    public CaptchaCodeStorage(long expirationInMinutes, int captchaLength) {
        this.captchaCodes = new ExpiringMap<>(expirationInMinutes, TimeUnit.MINUTES);
        this.captchaLength = captchaLength;
    }

    /**
     * Sets the expiration of captcha codes.
     *
     * @param expirationInMinutes minutes after which a saved captcha code expires
     */
    public void setExpirationInMinutes(long expirationInMinutes) {
        captchaCodes.setExpiration(expirationInMinutes, TimeUnit.MINUTES);
    }

    /**
     * Sets the captcha length.
     *
     * @param captchaLength number of characters a captcha code should have
     */
    public void setCaptchaLength(int captchaLength) {
        this.captchaLength = captchaLength;
    }

    /**
     * Returns the stored captcha for the player or generates and saves a new one.
     *
     * @param name the player's name
     * @return the code the player is required to enter
     */
    public String getCodeOrGenerateNew(String name) {
        String code = captchaCodes.get(name.toLowerCase(Locale.ROOT));
        return code == null ? generateCode(name) : code;
    }

    /**
     * Generates a code for the player and returns it.
     *
     * @param name the name of the player to generate a code for
     * @return the generated code
     */
    private String generateCode(String name) {
        String code = RandomStringUtils.generate(captchaLength);
        captchaCodes.put(name.toLowerCase(Locale.ROOT), code);
        return code;
    }

    /**
     * Checks the given code against the existing one. Upon success, the saved captcha code is removed from storage.
     * Upon failure, a new code is generated.
     *
     * @param name the name of the player to check
     * @param code the supplied code
     * @return true if the code matches, false otherwise
     */
    public boolean checkCode(String name, String code) {
        String nameLowerCase = name.toLowerCase(Locale.ROOT);
        String savedCode = captchaCodes.get(nameLowerCase);
        if (savedCode != null && savedCode.equalsIgnoreCase(code)) {
            captchaCodes.remove(nameLowerCase);
            return true;
        } else {
            generateCode(name);
        }
        return false;
    }

    public void removeExpiredEntries() {
        captchaCodes.removeExpiredEntries();
    }
}
