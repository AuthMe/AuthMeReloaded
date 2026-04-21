package fr.xephi.authme.listener;

import fr.xephi.authme.message.Messages;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import javax.inject.Inject;

@SuppressWarnings({"deprecation"})
public class PlayerListenerPre1217 implements Listener {

    @Inject
    private Messages messages;
    @Inject
    private OnJoinVerifier onJoinVerifier;

    // Note: We can't teleport the player in the PlayerLoginEvent listener
    // as the new player location will be reverted by the server.

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLogin(PlayerLoginEvent event) {
        var player = event.getPlayer();
        var name = player.getName();

        try {
            onJoinVerifier.checkSingleSession(name);
        } catch (FailedVerificationException e) {
            var message = LegacyComponentSerializer.legacySection().deserialize(
                messages.retrieveSingle(name, e.getReason(), e.getArgs())
            );
            event.kickMessage(message);
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL) {
            var check = new PlayerServerFullCheck(
                event.getPlayer().getPlayerProfile(),
                event.kickMessage(),
                event.getResult() == PlayerLoginEvent.Result.ALLOWED
            );
            onJoinVerifier.refusePlayerForFullServer(check);
            event.setResult(check.isAllowed() ? PlayerLoginEvent.Result.ALLOWED : PlayerLoginEvent.Result.KICK_FULL);
            event.kickMessage(check.getKickMessage());
        }
    }
}
