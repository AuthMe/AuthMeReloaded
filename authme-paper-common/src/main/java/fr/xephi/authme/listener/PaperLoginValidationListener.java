package fr.xephi.authme.listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import fr.xephi.authme.message.Messages;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Inject;

/**
 * Shared login validation listener for Paper-derived platforms.
 */
public class PaperLoginValidationListener implements Listener {

    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.legacySection();

    @Inject
    private OnJoinVerifier onJoinVerifier;

    @Inject
    private Messages messages;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerConnectionValidateLogin(PlayerConnectionValidateLoginEvent event) {
        if (!(event.getConnection() instanceof PlayerLoginConnection loginConnection)) {
            return;
        }

        String playerName = getPlayerName(loginConnection);
        if (playerName == null) {
            return;
        }

        try {
            onJoinVerifier.checkSingleSession(playerName);
        } catch (FailedVerificationException e) {
            String kickMessage = messages.retrieveSingle(playerName, e.getReason(), e.getArgs());
            event.kickMessage(LEGACY_SERIALIZER.deserialize(kickMessage));
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

    private static String getPlayerName(PlayerLoginConnection connection) {
        PlayerProfile profile = connection.getAuthenticatedProfile();
        if (profile != null && profile.getName() != null) {
            return profile.getName();
        }

        profile = connection.getUnsafeProfile();
        return profile == null ? null : profile.getName();
    }
}
