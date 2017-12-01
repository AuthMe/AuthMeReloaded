package fr.xephi.authme.data;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.expiring.ExpiringSet;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Captcha handler for registration.
 */
public class RegistrationCaptchaManager implements SettingsDependent, HasCleanup {

    private static final int MINUTES_VALID_FOR_REGISTRATION = 30;

    private final Map<String, String> captchaCodes;
    private final ExpiringSet<String> verifiedNamesForRegistration;

    private boolean isEnabledForRegistration;
    private int captchaLength;

    @Inject
    RegistrationCaptchaManager(Settings settings) {
        this.captchaCodes = new ConcurrentHashMap<>();
        this.verifiedNamesForRegistration = new ExpiringSet<>(MINUTES_VALID_FOR_REGISTRATION, TimeUnit.MINUTES);
        reload(settings);
    }

    /**
     * Returns whether the given player is required to solve a captcha before he can register.
     *
     * @param name the name of the player to verify
     * @return true if the player has to solve a captcha, false otherwise
     */
    public boolean isCaptchaRequired(String name) {
        return isEnabledForRegistration && !verifiedNamesForRegistration.contains(name.toLowerCase());
    }

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
        if (savedCode == null) {
            return true;
        } else if (savedCode.equalsIgnoreCase(code)) {
            captchaCodes.remove(nameLowerCase);
            verifiedNamesForRegistration.add(nameLowerCase);
            return true;
        }
        return false;
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabledForRegistration = settings.getProperty(SecuritySettings.ENABLE_CAPTCHA_FOR_REGISTRATION);
        this.captchaLength = settings.getProperty(SecuritySettings.CAPTCHA_LENGTH);
    }

    @Override
    public void performCleanup() {
        verifiedNamesForRegistration.removeExpiredEntries();
    }
}
