package fr.xephi.authme.service;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.initialization.TaskCloser;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.CancellableTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static fr.xephi.authme.task.FoliaCancellableTask.mapTask;
import static fr.xephi.authme.task.FoliaCancellableTask.mapConsumer;

public class FoliaBukkitService extends BukkitService {

    @Inject
    public FoliaBukkitService(AuthMe authMe, Settings settings) {
        super(authMe, settings);
    }

    @Override
    public @NotNull CancellableTask runOnAsyncSchedulerNow(@NotNull Consumer<CancellableTask> task) {
        return mapTask(Bukkit.getAsyncScheduler().runNow(authMe, mapConsumer(task)));
    }

    @Override
    public @NotNull CancellableTask runOnAsyncSchedulerDelayed(@NotNull Consumer<CancellableTask> task,
                                                             long delay,
                                                             @NotNull TimeUnit unit) {
        return mapTask(Bukkit.getAsyncScheduler().runDelayed(authMe, mapConsumer(task), delay, unit));
    }

    @Override
    public @NotNull CancellableTask runOnAsyncSchedulerAtFixedRate(@NotNull Consumer<CancellableTask> task,
                                                                 long initialDelay,
                                                                 long period,
                                                                 @NotNull TimeUnit unit) {
        return mapTask(Bukkit.getAsyncScheduler()
            .runAtFixedRate(authMe, mapConsumer(task), initialDelay, period, unit));
    }

    @Override
    public void cancelTasksOnAsyncScheduler() {
        Bukkit.getAsyncScheduler().cancelTasks(authMe);
    }

    @Override
    public void executeOnRegionScheduler(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable run) {
        Bukkit.getRegionScheduler().execute(authMe, world, chunkX, chunkZ, run);
    }

    @Override
    public void executeOptionallyOnRegionScheduler(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable run) {
        if (Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ)) {
            run.run();
        } else {
            executeOnRegionScheduler(world, chunkX, chunkZ, run);
        }
    }

    @Override
    public @NotNull CancellableTask runOnRegionScheduler(@NotNull World world,
                                                       int chunkX,
                                                       int chunkZ,
                                                       @NotNull Consumer<CancellableTask> task) {
        return mapTask(Bukkit.getRegionScheduler().run(authMe, world, chunkX, chunkZ, mapConsumer(task)));
    }

    @Override
    public @NotNull CancellableTask runOnRegionSchedulerDelayed(@NotNull World world,
                                                              int chunkX,
                                                              int chunkZ,
                                                              @NotNull Consumer<CancellableTask> task,
                                                              long delayTicks) {
        return mapTask(Bukkit.getRegionScheduler()
            .runDelayed(authMe, world, chunkX, chunkZ, mapConsumer(task), delayTicks));
    }

    @Override
    public @NotNull CancellableTask runOnRegionSchedulerAtFixedRate(@NotNull World world,
                                                                  int chunkX,
                                                                  int chunkZ,
                                                                  @NotNull Consumer<CancellableTask> task,
                                                                  long initialDelayTicks,
                                                                  long periodTicks) {
        return mapTask(Bukkit.getRegionScheduler()
            .runAtFixedRate(authMe, world, chunkX, chunkZ, mapConsumer(task), initialDelayTicks, periodTicks));
    }

    @Override
    public void executeOnGlobalRegionScheduler(@NotNull Runnable run) {
        Bukkit.getGlobalRegionScheduler().execute(authMe, run);
    }

    @Override
    public void executeOptionallyOnGlobalRegionScheduler(@NotNull Runnable run) {
        if (Bukkit.isGlobalTickThread()) {
            run.run();
        } else {
            executeOnGlobalRegionScheduler(run);
        }
    }

    @Override
    public @NotNull CancellableTask runOnGlobalRegionScheduler(@NotNull Consumer<CancellableTask> task) {
        return mapTask(Bukkit.getGlobalRegionScheduler().run(authMe, mapConsumer(task)));
    }

    @Override
    public @NotNull CancellableTask runOnGlobalRegionSchedulerDelayed(@NotNull Consumer<CancellableTask> task,
                                                                      long delayTicks) {
        return mapTask(Bukkit.getGlobalRegionScheduler().runDelayed(authMe, mapConsumer(task), delayTicks));
    }

    @Override
    public @NotNull CancellableTask runOnGlobalRegionSchedulerAtFixedRate(@NotNull Consumer<CancellableTask> task,
                                                                        long initialDelayTicks,
                                                                        long periodTicks) {
        return mapTask(Bukkit.getGlobalRegionScheduler()
            .runAtFixedRate(authMe, mapConsumer(task), initialDelayTicks, periodTicks));
    }

    @Override
    public void cancelTasksOnGlobalRegionScheduler() {
        Bukkit.getGlobalRegionScheduler().cancelTasks(authMe);
    }


    @Override
    public boolean executeOnEntityScheduler(@NotNull Entity entity,
                                            @NotNull Runnable run,
                                            @Nullable Runnable retired,
                                            long delay) {
        return entity.getScheduler().execute(authMe, run, retired, delay);
    }

    @Override
    public boolean executeOptionallyOnEntityScheduler(@NotNull Entity entity, @NotNull Runnable run, @Nullable Runnable retired) {
        if (Bukkit.isOwnedByCurrentRegion(entity)) {
            run.run();
            return true;
        } else {
            return executeOnEntityScheduler(entity, run, retired, 0L);
        }
    }

    @Override
    public @Nullable CancellableTask runOnEntityScheduler(@NotNull Entity entity,
                                       @NotNull Consumer<CancellableTask> task,
                                       @Nullable Runnable retired) {
        return mapTask(entity.getScheduler().run(authMe, mapConsumer(task), retired));
    }

    @Override
    public @Nullable CancellableTask runOnEntitySchedulerDelayed(@NotNull Entity entity,
                                       @NotNull Consumer<CancellableTask> task,
                                       @Nullable Runnable retired,
                                       long delayTicks) {
        return mapTask(entity.getScheduler().runDelayed(authMe, mapConsumer(task), retired, delayTicks));
    }

    @Override
    public @Nullable CancellableTask runOnEntitySchedulerAtFixedRate(@NotNull Entity entity,
                                                  @NotNull Consumer<CancellableTask> task,
                                                  @Nullable Runnable retired,
                                                  long initialDelayTicks,
                                                  long periodTicks) {
        return mapTask(entity.getScheduler()
            .runAtFixedRate(authMe, mapConsumer(task), retired, initialDelayTicks, periodTicks));
    }

    @Override
    public void waitAllTasks() {
        // todo: implement
    }

    @Override
    public void teleport(Player player, Location location) {
        player.teleportAsync(location);
    }
}
