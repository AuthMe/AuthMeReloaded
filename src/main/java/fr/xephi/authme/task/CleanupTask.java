package fr.xephi.authme.task;

import ch.jalu.injector.factory.SingletonStore;
import fr.xephi.authme.initialization.HasCleanup;
import org.bukkit.scheduler.BukkitRunnable;

import javax.inject.Inject;
import java.util.function.Consumer;

/**
 * Task run periodically to invoke the cleanup task on services.
 */
public class CleanupTask implements Consumer<CancellableTask> {

    @Inject
    private SingletonStore<HasCleanup> hasCleanupStore;

    CleanupTask() {
    }

    @Override
    public void accept(CancellableTask cancellableTask) {
        hasCleanupStore.retrieveAllOfType()
            .forEach(HasCleanup::performCleanup);
    }
}
