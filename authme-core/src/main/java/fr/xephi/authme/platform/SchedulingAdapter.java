package fr.xephi.authme.platform;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.service.CancellableTask;
import org.bukkit.entity.Entity;

/**
 * Platform-specific task scheduling bridge used by {@link fr.xephi.authme.service.BukkitService}.
 * Implementations map AuthMe's global and entity-owned task concepts to the appropriate
 * scheduler model of the running server platform.
 */
public interface SchedulingAdapter {

    /**
     * @param entity the entity that would own the task
     * @return true if the current thread may already safely interact with the entity
     */
    boolean isOwnedByCurrentThread(Entity entity);

    /**
     * @return true if the current thread may already safely perform global server work
     */
    boolean isGlobalThread();

    /**
     * Runs a task on the owning thread of the given entity.
     *
     * @param plugin the plugin scheduling the task
     * @param entity the entity that owns the execution context
     * @param task the task to run
     */
    void runOnEntityThread(AuthMe plugin, Entity entity, Runnable task);

    /**
     * Runs a delayed task on the owning thread of the given entity.
     *
     * @param plugin the plugin scheduling the task
     * @param entity the entity that owns the execution context
     * @param task the task to run
     * @param delay the delay in ticks
     * @return the scheduled task handle
     */
    CancellableTask runDelayedOnEntityThread(AuthMe plugin, Entity entity, Runnable task, long delay);

    /**
     * Runs a repeating task on the owning thread of the given entity.
     *
     * @param plugin the plugin scheduling the task
     * @param entity the entity that owns the execution context
     * @param task the task to run
     * @param delay the initial delay in ticks
     * @param period the repeat period in ticks
     * @return the scheduled task handle
     */
    CancellableTask runAtFixedRateOnEntityThread(AuthMe plugin, Entity entity, Runnable task, long delay, long period);

    /**
     * Runs a one-off background task that is not tied to a global or entity-owned thread.
     *
     * @param plugin the plugin scheduling the task
     * @param task the task to run
     * @return the scheduled task handle
     */
    CancellableTask runAsyncTask(AuthMe plugin, Runnable task);

    /**
     * Runs a repeating background task that is not tied to a global or entity-owned thread.
     *
     * @param plugin the plugin scheduling the task
     * @param task the task to run
     * @param delay the initial delay in ticks
     * @param period the repeat period in ticks
     * @return the scheduled task handle
     */
    CancellableTask runAsyncTaskTimer(AuthMe plugin, Runnable task, long delay, long period);

    /**
     * Runs a task on the platform's global execution context.
     *
     * @param plugin the plugin scheduling the task
     * @param task the task to run
     */
    void runOnGlobalThread(AuthMe plugin, Runnable task);

    /**
     * Runs a delayed task on the platform's global execution context.
     *
     * @param plugin the plugin scheduling the task
     * @param task the task to run
     * @param delay the delay in ticks
     * @return the scheduled task handle
     */
    CancellableTask runDelayedOnGlobalThread(AuthMe plugin, Runnable task, long delay);
}
