package fr.xephi.authme.service;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.util.expiring.ExpiringMap;
import fr.xephi.authme.util.expiring.TimedCounter;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Manager for recovery codes.
 */
public class RecoveryCodeService implements SettingsDependent, HasCleanup {

    private final ExpiringMap<String, String> recoveryCodes;
    private final TimedCounter<String> playerTries;
    private int recoveryCodeLength;
    private int recoveryCodeExpiration;
    private int recoveryCodeMaxTries;

    @Inject
    RecoveryCodeService(Settings settings) {
        recoveryCodeLength = settings.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH);
        recoveryCodeExpiration = settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID);
        recoveryCodeMaxTries = settings.getProperty(SecuritySettings.RECOVERY_CODE_MAX_TRIES);
        recoveryCodes = new ExpiringMap<>(recoveryCodeExpiration, TimeUnit.HOURS);
        playerTries = new TimedCounter<>(recoveryCodeExpiration, TimeUnit.HOURS);
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

        playerTries.put(player, recoveryCodeMaxTries);
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
        playerTries.decrement(player);
        return storedCode != null && storedCode.equals(code);
    }

    /**
     * Checks whether a player has tries remaining to enter a code.
     *
     * @param player The player to check for.
     * @return True if the player has tries left.
     */
    public boolean hasTriesLeft(String player) {
        return playerTries.get(player) > 0;
    }

    /**
     * Get the number of attempts a player has to enter a code.
     *
     * @param player The player to check for.
     * @return The number of tries left.
     */
    public int getTriesLeft(String player) {
        return playerTries.get(player);
    }

    /**
     * Removes the player's recovery code if present.
     *
     * @param player the player
     */
    public void removeCode(String player) {
        recoveryCodes.remove(player);
        playerTries.remove(player);
    }

    @Override
    public void reload(Settings settings) {
        recoveryCodeLength = settings.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH);
        recoveryCodeExpiration = settings.getProperty(SecuritySettings.RECOVERY_CODE_HOURS_VALID);
        recoveryCodeMaxTries = settings.getProperty(SecuritySettings.RECOVERY_CODE_MAX_TRIES);
        recoveryCodes.setExpiration(recoveryCodeExpiration, TimeUnit.HOURS);
    }

    @Override
    public void performCleanup() {
        recoveryCodes.removeExpiredEntries();
        playerTries.removeExpiredEntries();
    }
}
