package fr.xephi.authme.initialization;

/**
 * Common interface for types which have data that becomes outdated
 * and that can be cleaned up periodically.
 */
public interface HasCleanup {

    /**
     * Performs the cleanup action.
     */
    void performCleanup();

}
