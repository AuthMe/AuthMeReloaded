package fr.xephi.authme.listener;

import javax.inject.Inject;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPreLoginEvent;

import fr.xephi.authme.output.Messages;

/*
 *  This listener is registered only if the server is in offline-mode and the implementation is not Spigot.
 *  The reason is that only Spigot fires the Async version of this event on an offline-mode server!
 */
public class SyncSingleSessionListener implements Listener {

    @Inject
    private Messages m;
    @Inject
    private OnJoinVerifier onJoinVerifier;

    // Note: the PlayerPreLoginEvent causes the login thread to synchronize with the main thread
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.LOWEST)
    public void onSyncPreLogin(PlayerPreLoginEvent event) {
        if(event.getResult() != PlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        final String name = event.getName();

        try {
            onJoinVerifier.checkSingleSession(name);
        } catch (FailedVerificationException e) {
            event.setKickMessage(m.retrieveSingle(e.getReason(), e.getArgs()));
            event.setResult(PlayerPreLoginEvent.Result.KICK_OTHER);
            return;
        }
    }
}
