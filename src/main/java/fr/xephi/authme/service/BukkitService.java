package fr.xephi.authme.service;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.task.CancellableTask;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Service for operations requiring the Bukkit API, such as for scheduling.
 */
public abstract class BukkitService implements SettingsDependent {
    /**
     * Number of ticks per second in the Bukkit main thread.
     */
    public static final int TICKS_PER_SECOND = 20;
    /**
     * Number of milliseconds per tick in the Bukkit main thread.
     */
    public static final int MS_PER_TICK = 50;
    /**
     * Number of ticks per minute.
     */
    public static final int TICKS_PER_MINUTE = 60 * TICKS_PER_SECOND;
    protected final AuthMe authMe;
    private boolean useAsyncTasks;

    public BukkitService(AuthMe authMe, Settings settings) {
        this.authMe = authMe;
        reload(settings);
    }

    /**
     * Schedules the specified task to be executed asynchronously immediately.
     *
     * @param task Specified task.
     * @return The {@link CancellableTask} that represents the scheduled task.
     */
    public abstract @NotNull CancellableTask runOnAsyncSchedulerNow(@NotNull Consumer<CancellableTask> task);

    /**
     * Schedules the specified task to be executed asynchronously after the time delay has passed.
     *
     * @param task  Specified task.
     * @param delay The time delay to pass before the task should be executed.
     * @param unit  The time unit for the time delay.
     * @return The {@link CancellableTask} that represents the scheduled task.
     */
    public abstract @NotNull CancellableTask runOnAsyncSchedulerDelayed(@NotNull Consumer<CancellableTask> task,
                                                                      long delay,
                                                                      @NotNull TimeUnit unit);

    /**
     * Schedules the specified task to be executed asynchronously after the initial delay has passed,
     * and then periodically executed with the specified period.
     *
     * @param task         Specified task.
     * @param initialDelay The time delay to pass before the first execution of the task.
     * @param period       The time between task executions after the first execution of the task.
     * @param unit         The time unit for the initial delay and period.
     * @return The {@link CancellableTask} that represents the scheduled task.
     */
    public abstract @NotNull CancellableTask runOnAsyncSchedulerAtFixedRate(@NotNull Consumer<CancellableTask> task,
                                                                          long initialDelay,
                                                                          long period,
                                                                          @NotNull TimeUnit unit);

    /**
     * Attempts to cancel all tasks scheduled by the specified plugin.
     */
    public abstract void cancelTasksOnAsyncScheduler();

    /**
     * Schedules a task to be executed on the region which owns the location.
     *
     * @param world  The world of the region that owns the task
     * @param chunkX The chunk X coordinate of the region that owns the task
     * @param chunkZ The chunk Z coordinate of the region that owns the task
     * @param run    The task to execute
     */
    public abstract void executeOnRegionScheduler(@NotNull World world, int chunkX, int chunkZ, @NotNull Runnable run);

    /**
     * Schedules a task to be executed on the scheduler that owns the location.
     * It may run immediately.
     * @param world  The world of the region that owns the task
     * @param chunkX The chunk X coordinate of the region that owns the task
     * @param chunkZ The chunk Z coordinate of the region that owns the task
     * @param run    The task to execute
     */
    public abstract void executeOptionallyOnRegionScheduler(@NotNull World world,
                                                            int chunkX,
                                                            int chunkZ,
                                                            @NotNull Runnable run);

    /**
     * Schedules a task to be executed on the region which owns the location on the next tick.
     *
     * @param world  The world of the region that owns the task
     * @param chunkX The chunk X coordinate of the region that owns the task
     * @param chunkZ The chunk Z coordinate of the region that owns the task
     * @param task   The task to execute
     * @return The {@link CancellableTask} that represents the scheduled task.
     */
    public abstract @NotNull CancellableTask runOnRegionScheduler(@NotNull World world,
                                                                int chunkX,
                                                                int chunkZ,
                                                                @NotNull Consumer<CancellableTask> task);

    /**
     * Schedules a task to be executed on the region which owns the location after the specified delay in ticks.
     *
     * @param world      The world of the region that owns the task
     * @param chunkX     The chunk X coordinate of the region that owns the task
     * @param chunkZ     The chunk Z coordinate of the region that owns the task
     * @param task       The task to execute
     * @param delayTicks The delay, in ticks.
     * @return The {@link CancellableTask} that represents the scheduled task.
     */
    public abstract @NotNull CancellableTask runOnRegionSchedulerDelayed(@NotNull World world,
                                                                       int chunkX,
                                                                       int chunkZ,
                                                                       @NotNull Consumer<CancellableTask> task,
                                                                       long delayTicks);

    /**
     * Schedules a repeating task to be executed on the region which owns the location after the initial delay with the
     * specified period.
     *
     * @param world             The world of the region that owns the task
     * @param chunkX            The chunk X coordinate of the region that owns the task
     * @param chunkZ            The chunk Z coordinate of the region that owns the task
     * @param task              The task to execute
     * @param initialDelayTicks The initial delay, in ticks.
     * @param periodTicks       The period, in ticks.
     * @return The {@link CancellableTask} that represents the scheduled task.
     */
    public abstract @NotNull CancellableTask runOnRegionSchedulerAtFixedRate(@NotNull World world,
                                                                           int chunkX,
                                                                           int chunkZ,
                                                                           @NotNull Consumer<CancellableTask> task,
                                                                           long initialDelayTicks,
                                                                           long periodTicks);

    /**
     * Schedules a task to be executed on the global region.
     * @param run The task to execute
     */
    public abstract void executeOnGlobalRegionScheduler(@NotNull Runnable run);

    /**
     * Schedules a task to be executed on the global region.
     * It may run immediately.
     * @param run The task to execute
     */
    public abstract void executeOptionallyOnGlobalRegionScheduler(@NotNull Runnable run);

    /**
     * Schedules a task to be executed on the global region on the next tick.
     * @param task The task to execute
     * @return The {@link CancellableTask} that represents the scheduled task.
     */
    public abstract @NotNull CancellableTask runOnGlobalRegionScheduler(@NotNull Consumer<CancellableTask> task);

    /**
     * Schedules a task to be executed on the global region after the specified delay in ticks.
     * @param task The task to execute
     * @param delayTicks The delay, in ticks.
     * @return The {@link CancellableTask} that represents the scheduled task.
     */
    public abstract @NotNull CancellableTask runOnGlobalRegionSchedulerDelayed(@NotNull Consumer<CancellableTask> task,
                                                                             long delayTicks);

    /**
     * Schedules a repeating task to be executed on the global region after the initial delay with the
     * specified period.
     * @param task The task to execute
     * @param initialDelayTicks The initial delay, in ticks.
     * @param periodTicks The period, in ticks.
     * @return The {@link CancellableTask} that represents the scheduled task.
     */
    public abstract @NotNull CancellableTask runOnGlobalRegionSchedulerAtFixedRate(@NotNull Consumer<CancellableTask> task,
                                                                                 long initialDelayTicks,
                                                                                 long periodTicks);

    /**
     * Attempts to cancel all tasks scheduled by the specified plugin.
     */
    public abstract void cancelTasksOnGlobalRegionScheduler();

    /**
     * Schedules a task with the given delay. If the task failed to schedule because the scheduler is retired (entity
     * removed), then returns {@code false}. Otherwise, either the run callback will be invoked after the specified delay,
     * or the retired callback will be invoked if the scheduler is retired.
     * Note that the retired callback is invoked in critical code, so it should not attempt to remove the entity, remove
     * other entities, load chunks, load worlds, modify ticket levels, etc.
     *
     * <p>
     * It is guaranteed that the run and retired callback are invoked on the region which owns the entity.
     * </p>
     * @param entity The entity that owns the task
     * @param run The callback to run after the specified delay, may not be null.
     * @param retired Retire callback to run if the entity is retired before the run callback can be invoked, may be null.
     * @param delay The delay in ticks before the run callback is invoked. Any value less-than 1 is treated as 1.
     * @return {@code true} if the task was scheduled, which means that either the run function or the retired function
     *         will be invoked (but never both), or {@code false} indicating neither the run nor retired function will be invoked
     *         since the scheduler has been retired.
     */
    public abstract boolean executeOnEntityScheduler(@NotNull Entity entity,
                                                     @NotNull Runnable run,
                                                     @Nullable Runnable retired,
                                                     long delay);

    /**
     * Schedules a task to be executed on the entity scheduler.
     * It may run immediately.
     * @param run The task to execute
     * @param retired Retire callback to run if the entity is retired before the run callback can be invoked, may be null.
     * @return {@code true} if the task was scheduled, which means that either the run function or the retired function
     *         will be invoked (but never both), or {@code false} indicating neither the run nor retired function will be invoked
     *         since the scheduler has been retired.
     */
    public abstract boolean executeOptionallyOnEntityScheduler(@NotNull Entity entity,
                                                            @NotNull Runnable run,
                                                            @Nullable Runnable retired);

    /**
     * Schedules a task to execute on the next tick. If the task failed to schedule because the scheduler is retired (entity
     * removed), then returns {@code null}. Otherwise, either the task callback will be invoked after the specified delay,
     * or the retired callback will be invoked if the scheduler is retired.
     * Note that the retired callback is invoked in critical code, so it should not attempt to remove the entity, remove
     * other entities, load chunks, load worlds, modify ticket levels, etc.
     *
     * <p>
     * It is guaranteed that the task and retired callback are invoked on the region which owns the entity.
     * </p>
     * @param entity The entity that owns the task
     * @param task The task to execute
     * @param retired Retire callback to run if the entity is retired before the run callback can be invoked, may be null.
     * @return The {@link CancellableTask} that represents the scheduled task, or {@code null} if the entity has been removed.
     */
    public abstract @Nullable CancellableTask runOnEntityScheduler(@NotNull Entity entity,
                                                                 @NotNull Consumer<CancellableTask> task,
                                                                 @Nullable Runnable retired);

    /**
     * Schedules a task with the given delay. If the task failed to schedule because the scheduler is retired (entity
     * removed), then returns {@code null}. Otherwise, either the task callback will be invoked after the specified delay,
     * or the retired callback will be invoked if the scheduler is retired.
     * Note that the retired callback is invoked in critical code, so it should not attempt to remove the entity, remove
     * other entities, load chunks, load worlds, modify ticket levels, etc.
     *
     * <p>
     * It is guaranteed that the task and retired callback are invoked on the region which owns the entity.
     * </p>
     * @param entity The entity that owns the task
     * @param task The task to execute
     * @param retired Retire callback to run if the entity is retired before the run callback can be invoked, may be null.
     * @param delayTicks The delay, in ticks.
     * @return The {@link CancellableTask} that represents the scheduled task, or {@code null} if the entity has been removed.
     */
    public abstract @Nullable CancellableTask runOnEntitySchedulerDelayed(@NotNull Entity entity,
                                                                        @NotNull Consumer<CancellableTask> task,
                                                                        @Nullable Runnable retired,
                                                                        long delayTicks);

    /**
     * Schedules a repeating task with the given delay and period. If the task failed to schedule because the scheduler
     * is retired (entity removed), then returns {@code null}. Otherwise, either the task callback will be invoked after
     * the specified delay, or the retired callback will be invoked if the scheduler is retired.
     * Note that the retired callback is invoked in critical code, so it should not attempt to remove the entity, remove
     * other entities, load chunks, load worlds, modify ticket levels, etc.
     *
     * <p>
     * It is guaranteed that the task and retired callback are invoked on the region which owns the entity.
     * </p>
     * @param entity The entity that owns the task
     * @param task The task to execute
     * @param retired Retire callback to run if the entity is retired before the run callback can be invoked, may be null.
     * @param initialDelayTicks The initial delay, in ticks.
     * @param periodTicks The period, in ticks.
     * @return The {@link CancellableTask} that represents the scheduled task, or {@code null} if the entity has been removed.
     */
    public abstract @Nullable CancellableTask runOnEntitySchedulerAtFixedRate(@NotNull Entity entity,
                                                                            @NotNull Consumer<CancellableTask> task,
                                                                            @Nullable Runnable retired,
                                                                            long initialDelayTicks,
                                                                            long periodTicks);

    public abstract void waitAllTasks();

    /**
     * <b>Deprecated, use:
     * <ul>
     *     <li>{@link #runOnAsyncSchedulerNow}</li>
     *     <li>{@link #runOnGlobalRegionScheduler}</li>
     *     <li>{@link #runOnEntityScheduler}</li>
     * </ul>
     *
     * Schedules a once off task to occur as soon as possible.
     * <p>
     * This task will be executed by the main server thread.
     *
     * @param task Task to be executed
     * @return Task id number (-1 if scheduling failed)
     */
    @Deprecated
    public int scheduleSyncDelayedTask(Runnable task) {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(authMe, task);
    }

    /**
     * <b>Deprecated, use:
     * <ul>
     *     <li>{@link #runOnGlobalRegionSchedulerDelayed}</li>
     *     <li>{@link #runOnRegionSchedulerDelayed}</li>
     *     <li>{@link #runOnEntitySchedulerDelayed}</li>
     * </ul>
     *
     * Schedules a once off task to occur after a delay.
     * <p>
     * This task will be executed by the main server thread.
     *
     * @param task  Task to be executed
     * @param delay Delay in server ticks before executing task
     * @return Task id number (-1 if scheduling failed)
     */
    @Deprecated
    public int scheduleSyncDelayedTask(Runnable task, long delay) {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(authMe, task, delay);
    }

    /**
     * <b>Deprecated, use:
     * <ul>
     *     <li>{@link #executeOnGlobalRegionScheduler}</li>
     *     <li>{@link #executeOnRegionScheduler}</li>
     *     <li>{@link #executeOnEntityScheduler}</li>
     * </ul>
     *
     * Schedules a synchronous task if we are currently on a async thread; if not, it runs the task immediately.
     * Use this when {@link #runTaskOptionallyAsync(Runnable) optionally asynchronous tasks} have to
     * run something synchronously.
     *
     * @param task the task to be run
     */
    @Deprecated
    public void scheduleSyncTaskFromOptionallyAsyncTask(Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            scheduleSyncDelayedTask(task);
        }
    }

    /**
     * <b>Deprecated, use:
     * <ul>
     *     <li>{@link #runOnGlobalRegionScheduler}</li>
     *     <li>{@link #runOnRegionScheduler}</li>
     *     <li>{@link #runOnEntityScheduler}</li>
     * </ul>
     *
     * Returns a task that will run on the next server tick.
     *
     * @param task the task to be run
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalArgumentException if task is null
     */
    @Deprecated
    public BukkitTask runTask(Runnable task) {
        return Bukkit.getScheduler().runTask(authMe, task);
    }

    /**
     * <b>Deprecated, use:
     * <ul>
     *     <li>{@link #runOnGlobalRegionSchedulerDelayed}</li>
     *     <li>{@link #runOnRegionSchedulerDelayed}</li>
     *     <li>{@link #runOnEntitySchedulerDelayed}</li>
     * </ul>
     *
     * Returns a task that will run after the specified number of server
     * ticks.
     *
     * @param task  the task to be run
     * @param delay the ticks to wait before running the task
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalArgumentException if task is null
     */
    @Deprecated
    public BukkitTask runTaskLater(Runnable task, long delay) {
        return Bukkit.getScheduler().runTaskLater(authMe, task, delay);
    }

    /**
     * Schedules this task to run asynchronously or immediately executes it based on
     * AuthMe's configuration.
     *
     * @param task the task to run
     */
    public void runTaskOptionallyAsync(Runnable task) {
        if (useAsyncTasks) {
            runOnAsyncSchedulerNow(ignored -> task.run());
        } else {
            task.run();
        }
    }

    /**
     * <b>Deprecated, use:
     * <ul>
     *     <li>{@link #runOnAsyncSchedulerNow}</li>
     * </ul>
     *
     * <b>Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will run asynchronously.
     *
     * @param task the task to be run
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalArgumentException if task is null
     */
    @Deprecated
    public BukkitTask runTaskAsynchronously(Runnable task) {
        return Bukkit.getScheduler().runTaskAsynchronously(authMe, task);
    }

    /**
     * <b>Deprecated, use:
     * <ul>
     *     <li>{@link #runOnAsyncSchedulerAtFixedRate}</li>
     * </ul>
     * 
     * <b>Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will repeatedly run asynchronously until cancelled,
     * starting after the specified number of server ticks.
     *
     * @param task   the task to be run
     * @param delay  the ticks to wait before running the task for the first
     *               time
     * @param period the ticks to wait between runs
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if task is null
     * @throws IllegalStateException    if this was already scheduled
     */
    @Deprecated
    public BukkitTask runTaskTimerAsynchronously(BukkitRunnable task, long delay, long period) {
        return task.runTaskTimerAsynchronously(authMe, delay, period);
    }

    /**
     * <b>Deprecated, use:
     * <ul>
     *     <li>{@link #runOnGlobalRegionSchedulerAtFixedRate}</li>
     *     <li>{@link #runOnRegionSchedulerAtFixedRate}</li>
     *     <li>{@link #runOnEntitySchedulerAtFixedRate}</li>
     * </ul>
     *
     * Schedules the given task to repeatedly run until cancelled, starting after the
     * specified number of server ticks.
     *
     * @param task   the task to schedule
     * @param delay  the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalStateException    if this was already scheduled
     */
    @Deprecated
    public BukkitTask runTaskTimer(BukkitRunnable task, long delay, long period) {
        return task.runTaskTimer(authMe, delay, period);
    }

    /**
     * Broadcast a message to all players.
     *
     * @param message the message
     * @return the number of players
     */
    public int broadcastMessage(String message) {
        return Bukkit.broadcastMessage(message);
    }

    /**
     * Gets the player with the exact given name, case insensitive.
     *
     * @param name Exact name of the player to retrieve
     * @return a player object if one was found, null otherwise
     */
    public Player getPlayerExact(String name) {
        return authMe.getServer().getPlayerExact(name);
    }

    /**
     * Gets the player by the given name, regardless if they are offline or
     * online.
     * <p>
     * This method may involve a blocking web request to get the UUID for the
     * given name.
     * <p>
     * This will return an object even if the player does not exist. To this
     * method, all players will exist.
     *
     * @param name the name the player to retrieve
     * @return an offline player
     */
    public OfflinePlayer getOfflinePlayer(String name) {
        return authMe.getServer().getOfflinePlayer(name);
    }

    /**
     * Gets a set containing all banned players.
     *
     * @return a set containing banned players
     */
    public Set<OfflinePlayer> getBannedPlayers() {
        return Bukkit.getBannedPlayers();
    }

    /**
     * Gets every player that has ever played on this server.
     *
     * @return an array containing all previous players
     */
    public OfflinePlayer[] getOfflinePlayers() {
        return Bukkit.getOfflinePlayers();
    }

    /**
     * Gets a view of all currently online players.
     *
     * @return collection of online players
     */
    @SuppressWarnings("unchecked")
    public Collection<Player> getOnlinePlayers() {
        return (Collection<Player>) Bukkit.getOnlinePlayers();
    }

    /**
     * Calls an event with the given details.
     *
     * @param event Event details
     * @throws IllegalStateException Thrown when an asynchronous event is
     *                               fired from synchronous code.
     */
    public void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
    }

    /**
     * Creates an event with the provided function and emits it.
     *
     * @param eventSupplier the event supplier: function taking a boolean specifying whether AuthMe is configured
     *                      in async mode or not
     * @param <E>           the event type
     * @return the event that was created and emitted
     */
    public <E extends Event> E createAndCallEvent(Function<Boolean, E> eventSupplier) {
        E event = eventSupplier.apply(useAsyncTasks);
        callEvent(event);
        return event;
    }

    /**
     * Gets the world with the given name.
     *
     * @param name the name of the world to retrieve
     * @return a world with the given name, or null if none exists
     */
    public World getWorld(String name) {
        return Bukkit.getWorld(name);
    }

    /**
     * Dispatches a command on this server, and executes it if found.
     *
     * @param sender      the apparent sender of the command
     * @param commandLine the command + arguments. Example: <code>test abc 123</code>
     * @return returns false if no target is found
     */
    public boolean dispatchCommand(CommandSender sender, String commandLine) {
        return Bukkit.dispatchCommand(sender, commandLine);
    }

    /**
     * Dispatches a command to be run as console user on this server, and executes it if found.
     *
     * @param commandLine the command + arguments. Example: <code>test abc 123</code>
     * @return returns false if no target is found
     */
    public boolean dispatchConsoleCommand(String commandLine) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandLine);
    }

    @Override
    public void reload(Settings settings) {
        useAsyncTasks = settings.getProperty(PluginSettings.USE_ASYNC_TASKS);
    }

    /**
     * Send the specified bytes to bungeecord using the specified player connection.
     *
     * @param player the player
     * @param bytes  the message
     */
    public void sendBungeeMessage(Player player, byte[] bytes) {
        player.sendPluginMessage(authMe, "BungeeCord", bytes);
    }

    /**
     * Adds a ban to the list. If a previous ban exists, this will
     * update the previous entry.
     *
     * @param ip      the ip of the ban
     * @param reason  reason for the ban, null indicates implementation default
     * @param expires date for the ban's expiration (unban), or null to imply
     *                forever
     * @param source  source of the ban, null indicates implementation default
     * @return the entry for the newly created ban, or the entry for the
     * (updated) previous ban
     */
    public BanEntry banIp(String ip, String reason, Date expires, String source) {
        return Bukkit.getServer().getBanList(BanList.Type.IP).addBan(ip, reason, expires, source);
    }

    /**
     * Returns an optional with a boolean indicating whether bungeecord is enabled or not if the
     * server implementation is Spigot. Otherwise returns an empty optional.
     *
     * @return Optional with configuration value for Spigot, empty optional otherwise
     */
    public Optional<Boolean> isBungeeCordConfiguredForSpigot() {
        try {
            YamlConfiguration spigotConfig = Bukkit.spigot().getConfig();
            return Optional.of(spigotConfig.getBoolean("settings.bungeecord"));
        } catch (NoSuchMethodError e) {
            return Optional.empty();
        }
    }

    /**
     * @return the IP string that this server is bound to, otherwise empty string
     */
    public String getIp() {
        return Bukkit.getServer().getIp();
    }
}
