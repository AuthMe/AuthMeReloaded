package fr.xephi.authme.security.totp;

import com.google.common.primitives.Ints;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.IGoogleAuthenticator;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Provides TOTP functions (wrapping a third-party TOTP implementation).
 */
public class TotpAuthenticator {

    private final IGoogleAuthenticator authenticator;
    private final BukkitService bukkitService;

    @Inject
    TotpAuthenticator(BukkitService bukkitService) {
        this.authenticator = createGoogleAuthenticator();
        this.bukkitService = bukkitService;
    }

    /**
     * @return new Google Authenticator instance
     */
    protected IGoogleAuthenticator createGoogleAuthenticator() {
        return new GoogleAuthenticator();
    }

    public boolean checkCode(PlayerAuth auth, String totpCode) {
        return checkCode(auth.getTotpKey(), totpCode);
    }

    /**
     * Returns whether the given input code matches for the provided TOTP key.
     *
     * @param totpKey the key to check with
     * @param inputCode the input code to verify
     * @return true if code is valid, false otherwise
     */
    public boolean checkCode(String totpKey, String inputCode) {
        Integer totpCode = Ints.tryParse(inputCode);
        return totpCode != null && authenticator.authorize(totpKey, totpCode);
    }

    public TotpGenerationResult generateTotpKey(Player player) {
        GoogleAuthenticatorKey credentials = authenticator.createCredentials();
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
            bukkitService.getIp(), player.getName(), credentials);
        return new TotpGenerationResult(credentials.getKey(), qrCodeUrl);
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
