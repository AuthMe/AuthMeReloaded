package fr.xephi.authme.data;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.expiring.ExpiringMap;

import java.util.concurrent.TimeUnit;

/**
 * Manages captcha codes.
 */
public abstract class AbstractCaptchaManager implements SettingsDependent, HasCleanup {

    // Note: Proper expiration is set in reload(), which is also called on initialization
    private final ExpiringMap<String, String> captchaCodes = new ExpiringMap<>(0, TimeUnit.MINUTES);
    private int captchaLength;

    /**
     * Constructor.
     *
     * @param settings the settings instance
     */
    public AbstractCaptchaManager(Settings settings) {
        initialize(settings);
    }

    /**
     * Returns whether the given player is required to solve a captcha.
     *
     * @param name the name of the player to verify
     * @return true if the player has to solve a captcha, false otherwise
     */
    public abstract boolean isCaptchaRequired(String name);

    /**
     * Returns the stored captcha for the player or generates and saves a new one.
     *
     * @param name the player's name
     * @return the code the player is required to enter
     */
    public String getCaptchaCodeOrGenerateNew(String name) {
        String code = captchaCodes.get(name.toLowerCase());
        return code == null ? generateCode(name) : code;
    }

    /**
     * Generates a code for the player and returns it.
     *
     * @param name the name of the player to generate a code for
     * @return the generated code
     */
    public String generateCode(String name) {
        String code = RandomStringUtils.generate(captchaLength);
        captchaCodes.put(name.toLowerCase(), code);
        return code;
    }

    /**
     * Checks the given code against the existing one and resets the player's auth failure count upon success.
     *
     * @param name the name of the player to check
     * @param code the supplied code
     * @return true if the code matches or if no captcha is required for the player, false otherwise
     */
    public boolean checkCode(String name, String code) {
        final String nameLowerCase = name.toLowerCase();
        String savedCode = captchaCodes.get(nameLowerCase);
        if (savedCode != null && savedCode.equalsIgnoreCase(code)) {
            captchaCodes.remove(nameLowerCase);
            processSuccessfulCode(nameLowerCase);
            return true;
        } else {
            processUnsuccessfulCode(nameLowerCase);
        }
        return false;
    }

    private void initialize(Settings settings) {
        captchaLength = settings.getProperty(SecuritySettings.CAPTCHA_LENGTH);
        captchaCodes.setExpiration(minutesBeforeCodeExpires(settings), TimeUnit.MINUTES);
    }

    /**
     * Called on initialization and on reload.
     *
     * @param settings the settings instance
     */
    @Override
    public void reload(Settings settings) {
        // Note ljacqu 20171201: Use initialize() as an in-between method so that we can call it in the constructor
        // without causing any trouble to a child that may extend reload -> at the point of calling, the child's fields
        // would not yet be initialized.
        initialize(settings);
    }

    @Override
    public void performCleanup() {
        captchaCodes.removeExpiredEntries();
    }

    /**
     * Called when a player has successfully solved the captcha.
     *
     * @param nameLower the player's name (all lowercase)
     */
    protected abstract void processSuccessfulCode(String nameLower);

    /**
     * Called when a player has failed the captcha code.
     *
     * @param nameLower the player's name (all lowercase)
     */
    protected void processUnsuccessfulCode(String nameLower) {
    }

    /**
     * Returns the number of minutes a generated captcha code should live for before it may expire.
     *
     * @param settings the settings instance
     * @return number of minutes that the code is valid for
     */
    protected abstract int minutesBeforeCodeExpires(Settings settings);
}
