package fr.xephi.authme.data;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.ExpiringSet;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Captcha manager for registration.
 */
public class RegistrationCaptchaManager extends AbstractCaptchaManager {

    private static final int MINUTES_VALID_FOR_REGISTRATION = 30;

    private final ExpiringSet<String> verifiedNamesForRegistration =
        new ExpiringSet<>(MINUTES_VALID_FOR_REGISTRATION, TimeUnit.MINUTES);
    private boolean isEnabled;

    @Inject
    RegistrationCaptchaManager(Settings settings) {
        super(settings);
        reload(settings);
    }

    @Override
    public boolean isCaptchaRequired(String name) {
        return isEnabled && !verifiedNamesForRegistration.contains(name.toLowerCase());
    }

    @Override
    public void reload(Settings settings) {
        super.reload(settings);
        this.isEnabled = settings.getProperty(SecuritySettings.ENABLE_CAPTCHA_FOR_REGISTRATION);
    }

    @Override
    public void performCleanup() {
        super.performCleanup();
        verifiedNamesForRegistration.removeExpiredEntries();
    }

    @Override
    protected void processSuccessfulCode(String nameLower) {
        verifiedNamesForRegistration.add(nameLower);
    }

    @Override
    protected int minutesBeforeCodeExpires(Settings settings) {
        return MINUTES_VALID_FOR_REGISTRATION;
    }
}
