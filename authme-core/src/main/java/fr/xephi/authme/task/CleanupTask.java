package fr.xephi.authme.task;

import ch.jalu.injector.factory.SingletonStore;
import fr.xephi.authme.initialization.HasCleanup;

import javax.inject.Inject;

/**
 * Task run periodically to invoke the cleanup task on services.
 */
public class CleanupTask implements Runnable {

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
