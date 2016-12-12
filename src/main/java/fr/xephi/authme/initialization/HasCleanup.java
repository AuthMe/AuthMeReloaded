package fr.xephi.authme.initialization;

/**
 * Common interface for types which have data that becomes outdated
 * and that can be cleaned up periodically.
 *
 * @see fr.xephi.authme.task.CleanupTask
 */
public interface HasCleanup {

    /**
     * Performs the cleanup action.
     */
    void performCleanup();

}
