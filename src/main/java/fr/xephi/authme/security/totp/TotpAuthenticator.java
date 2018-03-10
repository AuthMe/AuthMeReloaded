package fr.xephi.authme.security.totp;

import com.google.common.annotations.VisibleForTesting;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import com.warrenstrange.googleauth.IGoogleAuthenticator;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Provides rudimentary TOTP functions (wraps third-party TOTP implementation).
 */
public class TotpAuthenticator {

    private final IGoogleAuthenticator authenticator;
    private final BukkitService bukkitService;


    @Inject
    TotpAuthenticator(BukkitService bukkitService) {
        this(new GoogleAuthenticator(), bukkitService);
    }

    @VisibleForTesting
    TotpAuthenticator(IGoogleAuthenticator authenticator, BukkitService bukkitService) {
        this.authenticator = authenticator;
        this.bukkitService = bukkitService;
    }

    public boolean checkCode(String totpKey, String inputCode) {
        try {
            Integer totpCode = Integer.valueOf(inputCode);
            return authenticator.authorize(totpKey, totpCode);
        } catch (NumberFormatException e) {
            // ignore
        }
        return false;
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

        TotpGenerationResult(String totpKey, String authenticatorQrCodeUrl) {
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
