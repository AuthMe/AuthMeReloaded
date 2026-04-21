package fr.xephi.authme.listener;

import fr.xephi.authme.message.Messages;
import io.papermc.paper.connection.PlayerLoginConnection;
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Inject;

public class PlayerListenerPost1217 implements Listener {

    @Inject
    private Messages messages;
    @Inject
    private OnJoinVerifier onJoinVerifier;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerConnectionValidateLogin(PlayerConnectionValidateLoginEvent event) {
        if (!(event.getConnection() instanceof PlayerLoginConnection loginConnection)) {
            return;
        }
        var profile = loginConnection.getUnsafeProfile();
        if (profile == null || profile.getName() == null) {
            throw new IllegalStateException("Incomplete profile in PlayerLoginConnection! (Should never happen)");
        }
        var name = profile.getName();

        try {
            onJoinVerifier.checkSingleSession(name);
        } catch (FailedVerificationException e) {
            var message = LegacyComponentSerializer.legacySection().deserialize(
                messages.retrieveSingle(name, e.getReason(), e.getArgs())
            );
            event.kickMessage(message);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerServerFullCheckEvent(PlayerServerFullCheckEvent event) {
        var check = new PlayerServerFullCheck(event.getPlayerProfile(), event.kickMessage(), event.isAllowed());
        onJoinVerifier.refusePlayerForFullServer(check);
        if (check.isAllowed()) {
            event.allow(true);
        } else {
            event.deny(check.getKickMessage());
        }
    }
}
