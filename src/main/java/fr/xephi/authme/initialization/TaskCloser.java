package fr.xephi.authme.initialization;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitWorker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Waits for asynchronous tasks to complete before closing the data source
 * so the plugin can shut down properly.
 */
public class TaskCloser implements Runnable {

    private final BukkitScheduler scheduler;
    private final Logger logger;
    private final AuthMe plugin;

    /**
     * Constructor.
     *
     * @param plugin the plugin instance
     */
    public TaskCloser(AuthMe plugin) {
        this.scheduler = plugin.getServer().getScheduler();
        this.logger = plugin.getLogger();
        this.plugin = plugin;
    }

    @Override
    public void run() {
        List<Integer> pendingTasks = getPendingTasks();
        logger.log(Level.INFO, "Waiting for {0} tasks to finish", pendingTasks.size());
        int progress = 0;

        //one minute + some time checking the running state
        int tries = 60;
        while (!pendingTasks.isEmpty()) {
            if (tries <= 0) {
                logger.log(Level.INFO, "Async tasks times out after to many tries {0}", pendingTasks);
                break;
            }

            try {
                sleep();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
                break;
            }

            for (Iterator<Integer> iterator = pendingTasks.iterator(); iterator.hasNext(); ) {
                int taskId = iterator.next();
                if (!scheduler.isCurrentlyRunning(taskId)) {
                    iterator.remove();
                    progress++;
                    logger.log(Level.INFO, "Progress: {0} / {1}", new Object[]{progress, pendingTasks.size()});
                }
            }

            tries--;
        }
    }

    /** Makes the current thread sleep for one second. */
    @VisibleForTesting
    void sleep() throws InterruptedException {
        Thread.sleep(1000);
    }

    private List<Integer> getPendingTasks() {
        List<Integer> pendingTasks = new ArrayList<>();
        //returns only the async tasks
        for (BukkitWorker pendingTask : scheduler.getActiveWorkers()) {
            if (pendingTask.getOwner().equals(plugin)
                //it's not a periodic task
                && !scheduler.isQueued(pendingTask.getTaskId())) {
                pendingTasks.add(pendingTask.getTaskId());
            }
        }
        return pendingTasks;
    }
}
