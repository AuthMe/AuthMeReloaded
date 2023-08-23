package fr.xephi.authme.security.totp;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.common.primitives.Ints;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.IGoogleAuthenticator;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.entity.Player;

import javax.inject.Inject;

import java.util.Locale;

import static fr.xephi.authme.util.Utils.MILLIS_PER_MINUTE;

/**
 * Provides TOTP functions (wrapping a third-party TOTP implementation).
 */
public class TotpAuthenticator implements HasCleanup {

    private static final int CODE_RETENTION_MINUTES = 5;

    private final IGoogleAuthenticator authenticator;
    private final Settings settings;
    private final Table<String, Integer, Long> usedCodes = HashBasedTable.create();

    @Inject
    TotpAuthenticator(Settings settings) {
        this.authenticator = createGoogleAuthenticator();
        this.settings = settings;
    }

    /**
     * @return new Google Authenticator instance
     */
    protected IGoogleAuthenticator createGoogleAuthenticator() {
        return new GoogleAuthenticator();
    }

    public boolean checkCode(PlayerAuth auth, String totpCode) {
        return checkCode(auth.getNickname(), auth.getTotpKey(), totpCode);
    }

    /**
     * Returns whether the given input code matches for the provided TOTP key.
     *
     * @param playerName the player name
     * @param totpKey the key to check with
     * @param inputCode the input code to verify
     * @return true if code is valid, false otherwise
     */
    public boolean checkCode(String playerName, String totpKey, String inputCode) {
        String nameLower = playerName.toLowerCase(Locale.ROOT);
        Integer totpCode = Ints.tryParse(inputCode);
        if (totpCode != null && !usedCodes.contains(nameLower, totpCode)
            && authenticator.authorize(totpKey, totpCode)) {
            usedCodes.put(nameLower, totpCode, System.currentTimeMillis());
            return true;
        }
        return false;
    }

    public TotpGenerationResult generateTotpKey(Player player) {
        GoogleAuthenticatorKey credentials = authenticator.createCredentials();
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
            settings.getProperty(PluginSettings.SERVER_NAME), player.getName(), credentials);
        return new TotpGenerationResult(credentials.getKey(), qrCodeUrl);
    }

    @Override
    public void performCleanup() {
        long threshold = System.currentTimeMillis() - CODE_RETENTION_MINUTES * MILLIS_PER_MINUTE;
        usedCodes.values().removeIf(value -> value < threshold);
    }

    public static final class TotpGenerationResult {
        private final String totpKey;
        private final String authenticatorQrCodeUrl;

        public TotpGenerationResult(String totpKey, String authenticatorQrCodeUrl) {
            this.totpKey = totpKey;
            this.authenticatorQrCodeUrl = authenticatorQrCodeUrl;
        }

        public String getTotpKey() {
            return totpKey;
        }

        public String getAuthenticatorQrCodeUrl() {
            return authenticatorQrCodeUrl;
        }
    }
}
