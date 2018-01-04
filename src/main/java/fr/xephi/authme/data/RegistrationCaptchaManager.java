package fr.xephi.authme.data;

import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.initialization.circulardependency.HasCircularDependency;
import fr.xephi.authme.initialization.circulardependency.InjectAfterInitialization;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.expiring.ExpiringSet;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Captcha manager for registration.
 */
public class RegistrationCaptchaManager extends AbstractCaptchaManager implements HasCircularDependency {

    private static final int MINUTES_VALID_FOR_REGISTRATION = 30;

    private LimboService limboService;

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
    protected void processUnsuccessfulCode(String nameLower) {
        final Player player = Bukkit.getPlayerExact(nameLower); // TODO #930: Pass in player!
        limboService.resetMessageTask(player, false);
    }

    @Override
    protected int minutesBeforeCodeExpires(Settings settings) {
        return MINUTES_VALID_FOR_REGISTRATION;
    }

    @InjectAfterInitialization
    public void setLimboService(LimboService limboService) {
        this.limboService = limboService;
    }
}
