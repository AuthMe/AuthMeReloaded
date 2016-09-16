package fr.xephi.authme.service;

import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.security.RandomString;
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
public class RecoveryCodeManager implements SettingsDependent {

    private Map<String, TimedEntry> recoveryCodes = new ConcurrentHashMap<>();

    private int recoveryCodeLength;
    private long recoveryCodeExpirationMillis;

    @Inject
    RecoveryCodeManager(Settings settings) {
        reload(settings);
    }

    public boolean isRecoveryCodeNeeded() {
        return recoveryCodeExpirationMillis > 0;
    }

    public String generateCode(String player) {
        String code = RandomString.generateHex(recoveryCodeLength);
        recoveryCodes.put(player, new TimedEntry(code, System.currentTimeMillis() + recoveryCodeExpirationMillis));
        return code;
    }

    public boolean isCodeValid(String player, String code) {
        TimedEntry entry = recoveryCodes.get(player);
        if (entry != null) {
            return code != null && code.equals(entry.getCode());
        }
        return false;
    }

    public void removeCode(String player) {
        recoveryCodes.remove(player);
    }

    @Override
    public void reload(Settings settings) {
        recoveryCodeLength = settings.getProperty(SecuritySettings.RECOVERY_CODE_LENGTH);
        recoveryCodeExpirationMillis = settings.getProperty(RECOVERY_CODE_HOURS_VALID) * MILLIS_PER_HOUR;
    }

    private static final class TimedEntry {

        private final String code;
        private final long expiration;

        TimedEntry(String code, long expiration) {
            this.code = code;
            this.expiration = expiration;
        }

        public String getCode() {
            return System.currentTimeMillis() < expiration ? code : null;
        }
    }

}
