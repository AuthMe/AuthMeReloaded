package fr.xephi.authme.listener;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import javax.inject.Inject;

/**
 * Shared async chat listener logic for Paper-derived platforms.
 */
public abstract class AbstractPaperAsyncChatListener implements Listener {

    @Inject
    private Settings settings;

    @Inject
    private Messages messages;

    @Inject
    private ListenerService listenerService;

    @Inject
    private PermissionsManager permissionsManager;

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncChatEvent event) {
        if (settings.getProperty(RestrictionSettings.ALLOW_CHAT)) {
            return;
        }

        Player player = event.getPlayer();
        boolean mayPlayerSendChat = !listenerService.shouldCancelEvent(player)
            || permissionsManager.hasPermission(player, PlayerStatePermission.ALLOW_CHAT_BEFORE_LOGIN);

        if (mayPlayerSendChat) {
            removeUnauthorizedViewers(event);
        } else {
            event.setCancelled(true);
            messages.send(player, MessageKey.DENIED_CHAT);
        }
    }

    private void removeUnauthorizedViewers(AsyncChatEvent event) {
        if (settings.getProperty(RestrictionSettings.HIDE_CHAT)) {
            event.viewers().removeIf(
                viewer -> viewer instanceof Player p && listenerService.shouldCancelEvent(p));
            if (event.viewers().isEmpty()) {
                event.setCancelled(true);
            }
        }
    }
}
