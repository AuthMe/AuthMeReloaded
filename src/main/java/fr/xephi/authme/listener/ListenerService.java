package fr.xephi.authme.listener;

import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;

/**
 * Service class for the AuthMe listeners.
 */
final class ListenerService {

    private ListenerService() {
    }

    /**
     * Return whether an event should be canceled (for unauthenticated, non-NPC players).
     *
     * @param event The event to process
     * @return True if the event should be canceled, false otherwise
     */
    public static boolean shouldCancelEvent(EntityEvent event) {
        Entity entity = event.getEntity();
        if (entity == null || !(entity instanceof Player)) {
            return false;
        }

        Player player = (Player) entity;
        return shouldCancelEvent(player);
    }

    /**
     * Return whether an event should be canceled (for unauthenticated, non-NPC players).
     *
     * @param event The event to process
     * @return True if the event should be canceled, false otherwise
     */
    public static boolean shouldCancelEvent(PlayerEvent event) {
        Player player = event.getPlayer();
        return shouldCancelEvent(player);
    }

    /**
     * Return, based on the player associated with the event, whether or not the event should be canceled.
     *
     * @param player The player to verify
     * @return True if the associated event should be canceled, false otherwise
     */
    public static boolean shouldCancelEvent(Player player) {
        return player != null && !Utils.checkAuth(player) && !Utils.isNPC(player);
    }

}
