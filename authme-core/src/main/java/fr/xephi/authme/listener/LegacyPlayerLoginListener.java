package fr.xephi.authme.listener;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import javax.inject.Inject;

/**
 * Handles the legacy Bukkit login event on platforms that still use it.
 */
public class LegacyPlayerLoginListener implements Listener {

    @Inject
    private Messages messages;
    @Inject
    private OnJoinVerifier onJoinVerifier;
    @Inject
    private ValidationService validationService;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        final String name = player.getName();

        try {
            onJoinVerifier.checkSingleSession(name);
        } catch (FailedVerificationException e) {
            event.setKickMessage(messages.retrieveSingle(name, e.getReason(), e.getArgs()));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        if (validationService.isUnrestricted(name)) {
            return;
        }

        onJoinVerifier.refusePlayerForFullServer(event);
    }
}
