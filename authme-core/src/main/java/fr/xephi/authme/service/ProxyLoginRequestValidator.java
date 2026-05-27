package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Locale;
import java.util.UUID;

/**
 * Validates premium data attached to proxy-initiated auto-login requests.
 *
 * <p>The backend only accepts a proxy-supplied Mojang UUID if it matches either the stored premium
 * UUID for the account or a currently pending premium enrollment being finalized. Any mismatch is
 * treated as an invalid premium claim and the auto-login request is rejected.</p>
 */
public class ProxyLoginRequestValidator {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(ProxyLoginRequestValidator.class);

    @Inject
    private DataSource dataSource;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private PendingPremiumCache pendingPremiumCache;

    @Inject
    private PremiumService premiumService;

    @Inject
    private BungeeSender bungeeSender;

    @Inject
    private Messages messages;

    ProxyLoginRequestValidator() {
    }

    /**
     * Validates an optional premium UUID coming from the proxy and finalizes pending premium
     * enrollment when the UUID was verified by the proxy and matches the pending entry.
     *
     * @param player the player to validate for
     * @param verifiedPremiumUuid the Mojang UUID verified by the proxy, or null for a regular session resume
     * @return true if the proxy login request can proceed, false otherwise
     */
    public boolean validate(Player player, UUID verifiedPremiumUuid) {
        if (verifiedPremiumUuid == null) {
            return true;
        }

        String playerName = player.getName();
        PlayerAuth auth = playerCache.getAuth(playerName);
        if (auth == null) {
            auth = dataSource.getAuth(playerName.toLowerCase(Locale.ROOT));
        }
        if (auth == null) {
            logger.warning("Rejected proxy premium login for " + playerName + ": no auth record found");
            return false;
        }

        if (auth.isPremium()) {
            if (verifiedPremiumUuid.equals(auth.getPremiumUuid())) {
                return true;
            }
            logger.warning("Rejected proxy premium login for " + playerName
                + ": verified UUID does not match stored premium UUID");
            return false;
        }

        UUID pendingPremiumUuid = pendingPremiumCache.removePending(playerName);
        if (pendingPremiumUuid == null) {
            logger.debug("No pending premium entry for " + playerName
                + " — possibly already processed by canBypassWithPremium");
            return false;
        }

        if (!verifiedPremiumUuid.equals(pendingPremiumUuid)) {
            logger.warning("Rejected proxy premium login for " + playerName
                + ": verified UUID does not match pending premium UUID");
            bungeeSender.sendPremiumUnset(playerName);
            messages.send(player, MessageKey.PREMIUM_PENDING_FAIL);
            return false;
        }

        premiumService.finalizePendingPremium(player, verifiedPremiumUuid);
        return true;
    }
}
