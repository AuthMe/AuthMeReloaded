package fr.xephi.authme.task;

import fr.xephi.authme.initialization.HasCleanup;
import fr.xephi.authme.initialization.factory.SingletonStore;
import org.bukkit.scheduler.BukkitRunnable;

import javax.inject.Inject;

/**
 * Task run periodically to invoke the cleanup task on services.
 */
public class CleanupTask extends BukkitRunnable {

    @Inject
    private SingletonStore<HasCleanup> hasCleanupStore;

    CleanupTask() {
    }

    @Override
    public void run() {
        hasCleanupStore.retrieveAllOfType()
            .forEach(HasCleanup::performCleanup);
    }
}
