package fr.xephi.authme.listener;

import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import com.destroystokyo.paper.profile.PlayerProfile;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.PendingConnectionRegistry;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConnection;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

/**
 * Shared login validation listener for Paper-derived platforms.
 */
public class PaperLoginValidationListener implements Listener {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    /** Extra time a name stays claimed on top of the configured login / register timeout. */
    private static final long CLAIM_GRACE_MILLIS = TimeUnit.SECONDS.toMillis(10);

    @Inject
    private OnJoinVerifier onJoinVerifier;

    @Inject
    private Messages messages;

    @Inject
    private PendingConnectionRegistry pendingConnectionRegistry;

    @Inject
    private Settings settings;

    // Paper fires this twice: in the login phase, and again once the connection is configured
    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerConnectionValidateLogin(PlayerConnectionValidateLoginEvent event) {
        PlayerConnection connection = event.getConnection();
        if (connection instanceof PlayerLoginConnection loginConnection) {
            verifyLoginPhase(event, loginConnection);
        } else if (connection instanceof PlayerConfigurationConnection configurationConnection) {
            verifyConfigurationPhase(event, configurationConnection);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerServerFullCheck(PlayerServerFullCheckEvent event) {
        if (event.isAllowed()) {
            return;
        }

        String playerName = event.getPlayerProfile().getName();
        if (playerName == null) {
            return;
        }

        String kickMessage = onJoinVerifier.getServerFullKickMessageIfDenied(playerName);
        if (kickMessage == null) {
            event.allow(true);
        } else {
            event.deny(LEGACY_SERIALIZER.deserialize(kickMessage));
        }
    }

    // From the play state on, checkSingleSession sees the player itself
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        pendingConnectionRegistry.release(event.getPlayer().getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerConnectionClose(PlayerConnectionCloseEvent event) {
        String playerName = event.getPlayerName();
        if (playerName != null) {
            pendingConnectionRegistry.releaseIfStale(playerName);
        }
    }

    private void verifyLoginPhase(PlayerConnectionValidateLoginEvent event, PlayerLoginConnection connection) {
        String playerName = getPlayerName(connection);
        if (playerName == null) {
            return;
        }

        try {
            onJoinVerifier.checkSingleSession(playerName);
        } catch (FailedVerificationException e) {
            denyConnection(event, playerName, e.getReason(), e.getArgs());
            return;
        }

        // A concurrent connection on the same name would take over its pre-join dialog state
        if (!pendingConnectionRegistry.tryClaim(playerName, connection, getClaimTtlMillis())) {
            denyConnection(event, playerName, MessageKey.USERNAME_ALREADY_ONLINE_ERROR);
        }
    }

    private void verifyConfigurationPhase(PlayerConnectionValidateLoginEvent event,
                                          PlayerConfigurationConnection connection) {
        PlayerProfile profile = connection.getProfile();
        String playerName = profile == null ? null : profile.getName();
        // No claim means an online player being reconfigured, which must not be checked against itself
        if (playerName == null || !pendingConnectionRegistry.holdsClaim(playerName, connection)) {
            return;
        }

        try {
            onJoinVerifier.checkSingleSession(playerName);
        } catch (FailedVerificationException e) {
            denyConnection(event, playerName, e.getReason(), e.getArgs());
            return;
        }

        // Renew so the claim covers the remaining wait until the player is placed in the world
        pendingConnectionRegistry.tryClaim(playerName, connection, getClaimTtlMillis());
    }

    private void denyConnection(PlayerConnectionValidateLoginEvent event, String playerName,
                                MessageKey reason, String... args) {
        event.kickMessage(LEGACY_SERIALIZER.deserialize(messages.retrieveSingle(playerName, reason, args)));
    }

    private long getClaimTtlMillis() {
        int timeoutSeconds = Math.max(settings.getProperty(RestrictionSettings.LOGIN_TIMEOUT),
            settings.getProperty(RestrictionSettings.REGISTER_TIMEOUT));
        return TimeUnit.SECONDS.toMillis(Math.max(timeoutSeconds, 0)) + CLAIM_GRACE_MILLIS;
    }

    private static String getPlayerName(PlayerLoginConnection connection) {
        PlayerProfile profile = connection.getAuthenticatedProfile();
        if (profile != null && profile.getName() != null) {
            return profile.getName();
        }

        profile = connection.getUnsafeProfile();
        return profile == null ? null : profile.getName();
    }
}
