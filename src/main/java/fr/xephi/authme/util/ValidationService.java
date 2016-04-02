package fr.xephi.authme.util;

import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;

/**
 * Validation service.
 */
public class ValidationService {

    private final NewSetting settings;

    public ValidationService(NewSetting settings) {
        this.settings = settings;
    }

    /**
     * Verifies whether a password is valid according to the plugin settings.
     *
     * @param password the password to verify
     * @param username the username the password is associated with
     * @return message key with the password error, or {@code null} if password is valid
     */
    public MessageKey validatePassword(String password, String username) {
        String passLow = password.toLowerCase();
        if (!passLow.matches(settings.getProperty(RestrictionSettings.ALLOWED_PASSWORD_REGEX))) {
            return MessageKey.PASSWORD_MATCH_ERROR;
        } else if (passLow.equalsIgnoreCase(username)) {
            return MessageKey.PASSWORD_IS_USERNAME_ERROR;
        } else if (password.length() < settings.getProperty(SecuritySettings.MIN_PASSWORD_LENGTH)
            || password.length() > settings.getProperty(SecuritySettings.MAX_PASSWORD_LENGTH)) {
            return MessageKey.INVALID_PASSWORD_LENGTH;
        } else if (settings.getProperty(SecuritySettings.UNSAFE_PASSWORDS).contains(passLow)) {
            // TODO #602 20160312: The UNSAFE_PASSWORDS should be all lowercase
            // -> introduce a lowercase String list property type
            return MessageKey.PASSWORD_UNSAFE_ERROR;
        }
        return null;
    }
}
