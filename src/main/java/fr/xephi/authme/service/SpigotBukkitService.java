package fr.xephi.authme.service;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.initialization.TaskCloser;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.BukkitCancellableTask;
import fr.xephi.authme.task.CancellableTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static fr.xephi.authme.task.BukkitCancellableTask.mapConsumer;

public class SpigotBukkitService extends BukkitService {

    @Inject
    public SpigotBukkitService(AuthMe authMe, Settings settings) {
        super(authMe, settings);
    }

    @Override
    public @NotNull CancellableTask runOnAsyncSchedulerNow(@NotNull Consumer<CancellableTask> task) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTaskAsynchronously(authMe, result.getConsumer());
        return result;
    }

    @Override
    public @NotNull CancellableTask runOnAsyncSchedulerDelayed(@NotNull Consumer<CancellableTask> task, long delay, @NotNull TimeUnit unit) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTaskLaterAsynchronously(authMe, result.getConsumer(), unit.toMillis(delay) / MS_PER_TICK);
        return result;
    }

    @Override
    public @NotNull CancellableTask runOnAsyncSchedulerAtFixedRate(@NotNull Consumer<CancellableTask> task, long initialDelay, long period, @NotNull TimeUnit unit) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTaskTimerAsynchronously(authMe, result.getConsumer(), unit.toMillis(initialDelay) / MS_PER_TICK, unit.toMillis(period) / MS_PER_TICK);
        return result;
    }

    @Override
    public void cancelTasksOnAsyncScheduler() {
        Bukkit.getScheduler().cancelTasks(authMe);
    }

    @Override
    public void executeOnRegionScheduler(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable run) {
        Bukkit.getScheduler().runTask(authMe, run);
    }

    @Override
    public void executeOptionallyOnRegionScheduler(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable run) {
        if (Bukkit.isPrimaryThread()) {
            run.run();
        } else {
            executeOnRegionScheduler(world, chunkX, chunkZ, run);
        }
    }

    @Override
    public @NotNull CancellableTask runOnRegionScheduler(@NotNull World world, int chunkX, int chunkZ, @NotNull Consumer<CancellableTask> task) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTask(authMe, mapConsumer(task));
        return result;
    }

    @Override
    public @NotNull CancellableTask runOnRegionSchedulerDelayed(@NotNull World world, int chunkX, int chunkZ, @NotNull Consumer<CancellableTask> task, long delayTicks) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTaskLater(authMe, result.getConsumer(), delayTicks);
        return result;
    }

    @Override
    public @NotNull CancellableTask runOnRegionSchedulerAtFixedRate(@NotNull World world, int chunkX, int chunkZ, @NotNull Consumer<CancellableTask> task, long initialDelayTicks, long periodTicks) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTaskTimer(authMe, result.getConsumer(), initialDelayTicks, periodTicks);
        return result;
    }

    @Override
    public void executeOnGlobalRegionScheduler(@NotNull Runnable run) {
        Bukkit.getScheduler().runTask(authMe, run);
    }

    @Override
    public void executeOptionallyOnGlobalRegionScheduler(@NotNull Runnable run) {
        if (Bukkit.isPrimaryThread()) {
            run.run();
        } else {
            executeOnGlobalRegionScheduler(run);
        }
    }

    @Override
    public @NotNull CancellableTask runOnGlobalRegionScheduler(@NotNull Consumer<CancellableTask> task) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTask(authMe, result.getConsumer());
        return result;
    }

    @Override
    public @NotNull CancellableTask runOnGlobalRegionSchedulerDelayed(@NotNull Consumer<CancellableTask> task, long delayTicks) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTaskLater(authMe, result.getConsumer(), delayTicks);
        return result;
    }

    @Override
    public @NotNull CancellableTask runOnGlobalRegionSchedulerAtFixedRate(@NotNull Consumer<CancellableTask> task, long initialDelayTicks, long periodTicks) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTaskTimer(authMe, result.getConsumer(), initialDelayTicks, periodTicks);
        return result;
    }

    @Override
    public void cancelTasksOnGlobalRegionScheduler() {
        Bukkit.getScheduler().cancelTasks(authMe);
    }

    @Override
    public boolean executeOnEntityScheduler(@NotNull Entity entity, @NotNull Runnable run, @Nullable Runnable retired, long delay) {
        if (delay <= 1) {
            Bukkit.getScheduler().runTask(authMe, run);
        } else {
            Bukkit.getScheduler().runTaskLater(authMe, run, delay);
        }
        return true;
    }

    @Override
    public boolean executeOptionallyOnEntityScheduler(@NotNull Entity entity, @NotNull Runnable run, @Nullable Runnable retired) {
        if (Bukkit.isPrimaryThread()) {
            run.run();
            return true;
        } else {
            return executeOnEntityScheduler(entity, run, retired, 0L);
        }
    }

    @Override
    public @Nullable CancellableTask runOnEntityScheduler(@NotNull Entity entity, @NotNull Consumer<CancellableTask> task, @Nullable Runnable retired) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTask(authMe, result.getConsumer());
        return result;
    }

    @Override
    public @Nullable CancellableTask runOnEntitySchedulerDelayed(@NotNull Entity entity, @NotNull Consumer<CancellableTask> task, @Nullable Runnable retired, long delayTicks) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTaskLater(authMe, result.getConsumer(), delayTicks);
        return result;
    }

    @Override
    public @Nullable CancellableTask runOnEntitySchedulerAtFixedRate(@NotNull Entity entity, @NotNull Consumer<CancellableTask> task, @Nullable Runnable retired, long initialDelayTicks, long periodTicks) {
        DeferredCancellableTask result = new DeferredCancellableTask(task);
        Bukkit.getScheduler().runTaskTimer(authMe, result.getConsumer(), initialDelayTicks, periodTicks);
        return result;
    }

    @Override
    public void teleport(Player player, Location location) {
        player.teleport(location);
    }

    @Override
    public void waitAllTasks() {
        new TaskCloser(authMe).run();
    }

    private static class DeferredCancellableTask implements CancellableTask {
        private final Consumer<BukkitTask> consumer;
        private volatile BukkitCancellableTask task = null;
        private volatile boolean cancelled;

        public DeferredCancellableTask(Consumer<CancellableTask> consumer) {
            this.consumer = new DeferredConsumer(consumer);
        }

        public Consumer<BukkitTask> getConsumer() {
            return consumer;
        }

        @Override
        public void cancel() {
            this.cancelled = true;
            if (task != null) {
                task.cancel();
            }
        }

        @Override
        public boolean isCancelled() {
            return cancelled || (task != null && task.isCancelled());
        }

        private class DeferredConsumer implements Consumer<BukkitTask> {
            private final Consumer<CancellableTask> consumer;

            public DeferredConsumer(Consumer<CancellableTask> consumer) {
                this.consumer = consumer;
            }

            @Override
            public void accept(BukkitTask bukkitTask) {
                if (cancelled) {
                    bukkitTask.cancel();
                    return;
                }
                BukkitCancellableTask bukkitCancellableTask = new BukkitCancellableTask(bukkitTask);
                task = bukkitCancellableTask;
                consumer.accept(bukkitCancellableTask);
            }
        }
    }
}
