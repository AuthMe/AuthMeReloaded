package fr.xephi.authme.service;

import org.bukkit.entity.Player;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Offers utility methods for testing involving a {@link BukkitService} mock.
 */
public final class BukkitServiceTestHelper {

    private BukkitServiceTestHelper() {
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link BukkitService#scheduleSyncTaskFromOptionallyAsyncTask}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(BukkitService bukkitService) {
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).scheduleSyncTaskFromOptionallyAsyncTask(any(Runnable.class));

        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(bukkitService).scheduleSyncTaskFromOptionallyAsyncTask(any(Player.class), any(Runnable.class));
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link BukkitService#runTaskAsynchronously}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToRunTaskAsynchronously(BukkitService bukkitService) {
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).runTaskAsynchronously(any(Runnable.class));
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link BukkitService#runTaskOptionallyAsync}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToRunTaskOptionallyAsync(BukkitService bukkitService) {
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).runTaskOptionallyAsync(any(Runnable.class));
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link BukkitService#scheduleSyncDelayedTask(Runnable)}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToScheduleSyncDelayedTask(BukkitService bukkitService) {
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).scheduleSyncDelayedTask(any(Runnable.class));
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link BukkitService#scheduleSyncDelayedTask(Runnable, long)}.
     *
     * @param bukkitService the mock to set behavior on
     */
    public static void setBukkitServiceToScheduleSyncDelayedTaskWithDelay(BukkitService bukkitService) {
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), anyLong());

        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(bukkitService).scheduleSyncDelayedTask(any(Player.class), any(Runnable.class), anyLong());
    }

    public static void setBukkitServiceToRunOnGlobalRegion(BukkitService bukkitService) {
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).runOnGlobalRegion(any(Runnable.class));
    }

    public static void setBukkitServiceToRunTaskLaterOnGlobalRegion(BukkitService bukkitService) {
        lenient().doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return mock(CancellableTask.class);
        }).when(bukkitService).runTaskLaterOnGlobalRegion(any(Runnable.class), anyLong());
    }
}
