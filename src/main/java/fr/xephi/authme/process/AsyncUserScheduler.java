package fr.xephi.authme.process;

import com.jano7.executor.KeySequentialRunner;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;

import javax.inject.Inject;

/**
 * Handles the queue of async tasks on a per-player basis.
 */
public class AsyncUserScheduler {

    @Inject
    private BukkitService bukkitService;

    private KeySequentialRunner<String> asyncUserScheduler;

    AsyncUserScheduler() {
        this.asyncUserScheduler = new KeySequentialRunner<>(command -> bukkitService.runTaskAsynchronously(command));
    }

    /**
     * Adds a task to the player's async task queue.
     *
     * @param playerName the player name.
     * @param runnable   the task.
     */
    public void runTask(String playerName, Runnable runnable) {
        if (bukkitService.isUseAsyncTasks()) {
            asyncUserScheduler.run(playerName.toLowerCase(), runnable);
        } else {
            runnable.run();
        }
    }

    /**
     * Adds a task to the player's async task queue.
     *
     * @param player   the player.
     * @param runnable the task.
     */
    public void runTask(Player player, Runnable runnable) {
        runTask(player.getName(), runnable);
    }

}
