package fr.xephi.authme.platform;

import org.bukkit.event.player.PlayerKickEvent;

/**
 * Platform-specific chat and kick behavior.
 * Paper 1.21+ overrides {@link #getKickReason} to use the Adventure API.
 */
public interface ChatAdapter {

    default String getKickReason(PlayerKickEvent event) {
        return event.getReason();
    }
}
