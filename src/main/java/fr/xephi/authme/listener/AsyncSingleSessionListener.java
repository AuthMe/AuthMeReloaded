package fr.xephi.authme.listener;

import javax.inject.Inject;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import fr.xephi.authme.output.Messages;

/*
 *  This listener is registered only if the server is in online-mode or the implementation is Spigot.
 *  The reason is that only Spigot fires the Async version of the event on an offline-mode server!
 */
public class AsyncSingleSessionListener implements Listener {

    @Inject
    private Messages m;
    @Inject
    private OnJoinVerifier onJoinVerifier;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSyncPreLogin(AsyncPlayerPreLoginEvent event) {
        if(event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }

        final String name = event.getName();

        try {
            onJoinVerifier.checkSingleSession(name);
        } catch (FailedVerificationException e) {
            event.setKickMessage(m.retrieveSingle(e.getReason(), e.getArgs()));
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            return;
        }
    }
}
