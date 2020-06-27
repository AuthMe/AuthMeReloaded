package fr.xephi.authme.task;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

/**
 * Message shown to a player in a regular interval as long as he is not logged in.
 */
public class MessageTask extends BukkitRunnable {

    @NotNull
    private final Player player;
    @NotNull
    private final String[] message;
    private boolean isMuted;

    /*
     * Constructor.
     */
    public MessageTask(@NotNull Player player, @NotNull String[] lines) {
        this.player = player;
        this.message = lines;
        isMuted = false;
    }

    public void setMuted(boolean isMuted) {
        this.isMuted = isMuted;
    }

    @Override
    public void run() {
        if (!isMuted) {
            player.sendMessage(message);
        }
    }
}
