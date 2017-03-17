package fr.xephi.authme.service;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.expiring.ExpiringMap;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static fr.xephi.authme.settings.properties.SecuritySettings.RECOVERY_CODE_HOURS_VALID;

/**
 * Manager for recovery codes.
 */
public class RecoveryCodeService implements SettingsDependent, HasCleanup {

    private final ExpiringMap<String, String> recoveryCodes;
    private int recoveryCodeLength;
    private int recoveryCodeExpiration;

    @Inject
    RecoveryCodeService(Settings settings) {
        recoveryCodeLength = settings.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH);
        recoveryCodeExpiration = settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID);
        recoveryCodes = new ExpiringMap<>(recoveryCodeExpiration, TimeUnit.HOURS);
    }

    /**
     * @return whether recovery codes are enabled or not
     */
    public boolean isRecoveryCodeNeeded() {
        return recoveryCodeLength > 0 && recoveryCodeExpiration > 0;
    }

    /**
     * Generates the recovery code for the given player.
     *
     * @param player the player to generate a code for
     * @return the generated code
     */
    public String generateCode(String player) {
        String code = RandomStringUtils.generateHex(recoveryCodeLength);
        recoveryCodes.put(player, code);
        return code;
    }

    /**
     * Checks whether the supplied code is valid for the given player.
     *
     * @param player the player to check for
     * @param code the code to check
     * @return true if the code matches and has not expired, false otherwise
     */
    public boolean isCodeValid(String player, String code) {
        String storedCode = recoveryCodes.get(player);
        return storedCode != null && storedCode.equals(code);
    }

    /**
     * Removes the player's recovery code if present.
     *
     * @param player the player
     */
    public void removeCode(String player) {
        recoveryCodes.remove(player);
    }

    @Override
    public void reload(Settings settings) {
        recoveryCodeLength = settings.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH);
        recoveryCodeExpiration = settings.getProperty(RECOVERY_CODE_HOURS_VALID);
        recoveryCodes.setExpiration(recoveryCodeExpiration, TimeUnit.HOURS);
    }

    @Override
    public void performCleanup() {
        recoveryCodes.removeExpiredEntries();
    }
}
