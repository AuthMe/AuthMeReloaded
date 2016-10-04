package fr.xephi.authme.service;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.util.RandomStringUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;

import javax.inject.Inject;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.settings.properties.SecuritySettings.RECOVERY_CODE_HOURS_VALID;
import static fr.xephi.authme.util.Utils.MILLIS_PER_HOUR;

/**
 * Manager for recovery codes.
 */
public class RecoveryCodeService implements SettingsDependent {

    private Map<String, ExpiringEntry> recoveryCodes = new ConcurrentHashMap<>();

    private int recoveryCodeLength;
    private long recoveryCodeExpirationMillis;

    @Inject
    RecoveryCodeService(Settings settings) {
        reload(settings);
    }

    /**
     * @return whether recovery codes are enabled or not
     */
    public boolean isRecoveryCodeNeeded() {
        return recoveryCodeLength > 0 && recoveryCodeExpirationMillis > 0;
    }

    /**
     * Generates the recovery code for the given player.
     *
     * @param player the player to generate a code for
     * @return the generated code
     */
    public String generateCode(String player) {
        String code = RandomStringUtils.generateHex(recoveryCodeLength);
        recoveryCodes.put(player, new ExpiringEntry(code, System.currentTimeMillis() + recoveryCodeExpirationMillis));
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
        ExpiringEntry entry = recoveryCodes.get(player);
        if (entry != null) {
            return code != null && code.equals(entry.getCode());
        }
        return false;
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
        recoveryCodeExpirationMillis = settings.getProperty(RECOVERY_CODE_HOURS_VALID) * MILLIS_PER_HOUR;
    }

    /**
     * Entry with an expiration.
     */
    @VisibleForTesting
    static final class ExpiringEntry {

        private final String code;
        private final long expiration;

        ExpiringEntry(String code, long expiration) {
            this.code = code;
            this.expiration = expiration;
        }

        String getCode() {
            return System.currentTimeMillis() < expiration ? code : null;
        }
    }

}
