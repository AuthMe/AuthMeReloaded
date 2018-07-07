package fr.xephi.authme.service;

import org.bukkit.entity.Player;

import java.util.Collection;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;

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
        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(0);
            runnable.run();
            return null;
        }).when(bukkitService).scheduleSyncTaskFromOptionallyAsyncTask(any(Runnable.class));
    }

    /**
     * Sets a BukkitService mock to run any Runnable it is passed to its method
     * {@link BukkitService#runTaskAsynchronously}.
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
     * {@link BukkitService#runTaskOptionallyAsync}.
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
     * {@link BukkitService#scheduleSyncDelayedTask(Runnable)}.
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
     * {@link BukkitService#scheduleSyncDelayedTask(Runnable, long)}.
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

    /**
     * Sets a BukkitService mock to return the given players when its method
     * {@link BukkitService#getOnlinePlayers()} is invoked.
     *
     * @param bukkitService the mock to set behavior on
     * @param players the players to return
     */
    @SuppressWarnings("unchecked")
    public static void returnGivenOnlinePlayers(BukkitService bukkitService, Collection<Player> players) {
        // The compiler gets lost in generics because Collection<? extends Player> is returned from getOnlinePlayers()
        given(bukkitService.getOnlinePlayers()).willReturn((Collection) players);
    }
}
