package fr.xephi.authme.service;

import org.bukkit.entity.Player;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks which players currently have an AuthMe-owned dialog open.
 * Used to ensure we only close dialogs that we opened, not dialogs from other plugins.
 */
public class DialogStateService {

    private final Set<UUID> playersWithOpenDialog = ConcurrentHashMap.newKeySet();

    /**
     * Records that an AuthMe dialog was shown to the given player.
     *
     * @param player the player who was shown a dialog
     */
    public void markDialogOpen(Player player) {
        playersWithOpenDialog.add(player.getUniqueId());
    }

    /**
     * Removes the dialog-open record for the given player.
     * Returns {@code true} if the player had an AuthMe dialog tracked (and it was removed),
     * or {@code false} if there was no record (i.e. no AuthMe dialog was open for that player).
     *
     * @param player the player to clear
     * @return true if an AuthMe dialog was tracked for this player
     */
    public boolean clearDialogOpen(Player player) {
        return playersWithOpenDialog.remove(player.getUniqueId());
    }
}
