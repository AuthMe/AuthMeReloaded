package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.Argon2;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;

import javax.inject.Inject;

/**
 * Logs warning messages in cases where the configured values suggest a misconfiguration.
 * <p>
 * Note that this class does not modify any settings and it is called after the settings have been fully loaded.
 * For actual migrations (= verifications which trigger changes and a resave of the settings),
 * see {@link SettingsMigrationService}.
 */
public class SettingsWarner {

    @Inject
    private Settings settings;

    @Inject
    private AuthMe authMe;

    SettingsWarner() {
    }

    /**
     * Logs warning when necessary to notify the user about misconfigurations.
     */
    public void logWarningsForMisconfigurations() {
        // Force single session disabled
        if (!settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)) {
            ConsoleLogger.warning("WARNING!!! By disabling ForceSingleSession, your server protection is inadequate!");
        }

        // Use TLS property only affects port 25
        if (!settings.getProperty(EmailSettings.PORT25_USE_TLS)
            && settings.getProperty(EmailSettings.SMTP_PORT) != 25) {
            ConsoleLogger.warning("Note: You have set Email.useTls to false but this only affects mail over port 25");
        }

        // Output hint if sessions are enabled that the timeout must be positive
        if (settings.getProperty(PluginSettings.SESSIONS_ENABLED)
            && settings.getProperty(PluginSettings.SESSIONS_TIMEOUT) <= 0) {
            ConsoleLogger.warning("Warning: Session timeout needs to be positive in order to work!");
        }

        // Check if argon2 library is present and can be loaded
        if (settings.getProperty(SecuritySettings.PASSWORD_HASH).equals(HashAlgorithm.ARGON2)
            && !Argon2.isLibraryLoaded()) {
            ConsoleLogger.warning("WARNING!!! You use Argon2 Hash Algorithm method but we can't find the Argon2 "
                + "library on your system! See https://github.com/AuthMe/AuthMeReloaded/wiki/Argon2-as-Password-Hash");
            authMe.stopOrUnload();
        }
    }
}
