package fr.xephi.authme.task;

import ch.jalu.injector.Injector;
import fr.xephi.authme.initialization.HasCleanup;
import org.bukkit.scheduler.BukkitRunnable;

import javax.inject.Inject;

/**
 * Task run periodically to invoke the cleanup task on services.
 */
public class CleanupTask extends BukkitRunnable {

    @Inject
    private Injector injector;

    CleanupTask() {
    }

    @Override
    public void run() {
        for (HasCleanup service : injector.retrieveAllOfType(HasCleanup.class)) {
            service.performCleanup();
        }
    }
}
