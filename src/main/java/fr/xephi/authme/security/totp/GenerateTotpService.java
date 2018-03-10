package fr.xephi.authme.security.totp;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.security.totp.TotpAuthenticator.TotpGenerationResult;
import fr.xephi.authme.util.expiring.ExpiringMap;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Handles the generation of new TOTP secrets for players.
 */
public class GenerateTotpService implements HasCleanup {

    private static final int NEW_TOTP_KEY_EXPIRATION_MINUTES = 5;

    private final ExpiringMap<String, TotpGenerationResult> totpKeys;

    @Inject
    private TotpAuthenticator totpAuthenticator;

    GenerateTotpService() {
        this.totpKeys = new ExpiringMap<>(NEW_TOTP_KEY_EXPIRATION_MINUTES, TimeUnit.MINUTES);
    }

    /**
     * Generates a new TOTP key and returns the corresponding QR code.
     *
     * @param player the player to save the TOTP key for
     * @return TOTP generation result
     */
    public TotpGenerationResult generateTotpKey(Player player) {
        TotpGenerationResult credentials = totpAuthenticator.generateTotpKey(player);
        totpKeys.put(player.getName().toLowerCase(), credentials);
        return credentials;
    }

    /**
     * Returns the generated TOTP secret of a player, if available and not yet expired.
     *
     * @param player the player to retrieve the TOTP key for
     * @return TOTP generation result
     */
    public TotpGenerationResult getGeneratedTotpKey(Player player) {
        return totpKeys.get(player.getName().toLowerCase());
    }

    public void removeGenerateTotpKey(Player player) {
        totpKeys.remove(player.getName().toLowerCase());
    }

    /**
     * Returns whether the given totp code is correct for the generated TOTP key of the player.
     *
     * @param player the player to verify the code for
     * @param totpCode the totp code to verify with the generated secret
     * @return true if the input code is correct, false if the code is invalid or no unexpired totp key is available
     */
    public boolean isTotpCodeCorrectForGeneratedTotpKey(Player player, String totpCode) {
        TotpGenerationResult totpDetails = totpKeys.get(player.getName().toLowerCase());
        return totpDetails != null && totpAuthenticator.checkCode(totpDetails.getTotpKey(), totpCode);
    }

    @Override
    public void performCleanup() {
        totpKeys.removeExpiredEntries();
    }
}
