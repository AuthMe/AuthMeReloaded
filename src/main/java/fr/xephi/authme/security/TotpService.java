package fr.xephi.authme.security;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.util.expiring.ExpiringMap;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Service for TOTP actions.
 */
public class TotpService implements HasCleanup {

    private static final int NEW_TOTP_KEY_EXPIRATION_MINUTES = 5;

    private final ExpiringMap<String, String> totpKeys;
    private final GoogleAuthenticator authenticator;
    private final BukkitService bukkitService;

    @Inject
    TotpService(BukkitService bukkitService) {
        this.bukkitService = bukkitService;
        this.totpKeys = new ExpiringMap<>(NEW_TOTP_KEY_EXPIRATION_MINUTES, TimeUnit.MINUTES);
        this.authenticator = new GoogleAuthenticator();
    }

    /**
     * Generates a new TOTP key and returns the corresponding QR code. The generated key is saved temporarily
     * for the user and can be later retrieved with a confirmation code from {@link #confirmCodeForGeneratedTotpKey}.
     *
     * @param player the player to save the TOTP key for
     * @return TOTP generation result
     */
    public TotpGenerationResult generateTotpKey(Player player) {
        GoogleAuthenticatorKey credentials = authenticator.createCredentials();
        totpKeys.put(player.getName().toLowerCase(), credentials.getKey());
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
            bukkitService.getIp(), player.getName(), credentials);
        return new TotpGenerationResult(credentials.getKey(), qrCodeUrl);
    }

    /**
     * Returns the generated TOTP secret of a player, if available and not yet expired.
     *
     * @param player the player to retrieve the TOTP key for
     * @return the totp secret
     */
    public String retrieveGeneratedSecret(Player player) {
        return totpKeys.get(player.getName().toLowerCase());
    }

    /**
     * Returns if the new totp code matches the newly generated totp key.
     *
     * @param player the player to retrieve the code for
     * @param totpCodeConfirmation the input code confirmation
     * @return the TOTP secret that was generated for the player, or null if not available or if the code is incorrect
     */
    // Maybe by allowing to retrieve without confirmation and exposing verifyCode(String, String)
    public boolean confirmCodeForGeneratedTotpKey(Player player, String totpCodeConfirmation) {
        String totpSecret = totpKeys.get(player.getName().toLowerCase());
        if (totpSecret != null) {
            if (checkCode(totpSecret, totpCodeConfirmation)) {
                totpKeys.remove(player.getName().toLowerCase());
                return true;
            }
        }
        return false;
    }

    public boolean verifyCode(PlayerAuth auth, String totpCode) {
        return checkCode(auth.getTotpKey(), totpCode);
    }

    private boolean checkCode(String totpKey, String inputCode) {
        try {
            Integer totpCode = Integer.valueOf(inputCode);
            return authenticator.authorize(totpKey, totpCode);
        } catch (NumberFormatException e) {
            // ignore
        }
        return false;
    }

    @Override
    public void performCleanup() {
        totpKeys.removeExpiredEntries();
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
