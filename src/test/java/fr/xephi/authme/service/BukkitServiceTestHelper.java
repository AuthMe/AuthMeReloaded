package fr.xephi.authme.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;

/**
 * Offers utility methods for testing involving a {@link FoliaBukkitService} mock.
 */
public final class BukkitServiceTestHelper {

    private BukkitServiceTestHelper() {
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link FoliaBukkitService#scheduleSyncTaskFromOptionallyAsyncTask}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(BukkitService bukkitService) {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).scheduleSyncTaskFromOptionallyAsyncTask(any(Runnable.class));
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link FoliaBukkitService#runTaskAsynchronously}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToRunTaskAsynchronously(BukkitService bukkitService) {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).runTaskAsynchronously(any(Runnable.class));
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link FoliaBukkitService#runTaskOptionallyAsync}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToRunTaskOptionallyAsync(BukkitService bukkitService) {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).runTaskOptionallyAsync(any(Runnable.class));
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link FoliaBukkitService#scheduleSyncDelayedTask(Runnable)}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToScheduleSyncDelayedTask(BukkitService bukkitService) {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).scheduleSyncDelayedTask(any(Runnable.class));
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link FoliaBukkitService#scheduleSyncDelayedTask(Runnable, long)}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToScheduleSyncDelayedTaskWithDelay(BukkitService bukkitService) {
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), anyLong());
    }
}
