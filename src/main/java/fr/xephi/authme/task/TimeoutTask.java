package fr.xephi.authme.task;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.entity.Player;

public class TimeoutTask implements Runnable {

    private final String name;
    private final Messages m;
    private final Player player;

    /**
     * Constructor for TimeoutTask.
     *
     * @param plugin AuthMe
     * @param name   String
     * @param player Player
     */
    public TimeoutTask(AuthMe plugin, String name, Player player) {
        this.m = plugin.getMessages();
        this.name = name;
        this.player = player;
    }

    @Override
    public void run() {
        if (!PlayerCache.getInstance().isAuthenticated(name)) {
            player.kickPlayer(m.retrieveSingle(MessageKey.LOGIN_TIMEOUT_ERROR));
        }
    }
}
