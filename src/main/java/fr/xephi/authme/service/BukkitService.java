package fr.xephi.authme.service;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.Utils;
import org.bukkit.BanEntry;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Service for operations requiring the Bukkit API, such as for scheduling.
 */
public class BukkitService implements SettingsDependent {

    /** Number of ticks per second in the Bukkit main thread. */
    public static final int TICKS_PER_SECOND = 20;
    /** Number of ticks per minute. */
    public static final int TICKS_PER_MINUTE = 60 * TICKS_PER_SECOND;

    private final AuthMe authMe;
    private final boolean getOnlinePlayersIsCollection;
    private Method getOnlinePlayers;
    private boolean useAsyncTasks;

    // Async executor
    private ThreadPoolExecutor asyncExecutor;

    @Inject
    BukkitService(AuthMe authMe, Settings settings) {
        this.authMe = authMe;
        getOnlinePlayersIsCollection = initializeOnlinePlayersIsCollectionField();

        int coreCount = Utils.getCoreCount();
        // Keep 1 free core for the main thread and the OS
        if(coreCount != 1) {
            coreCount--;
        }
        asyncExecutor = new ThreadPoolExecutor(coreCount, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
            new SynchronousQueue<>());

        reload(settings);
    }

    /**
     * Wait the shutdown of the async execution pool.
     *
     * @throws InterruptedException if the shutdown is interrupted
     */
    public void closeAsyncPool() throws InterruptedException {
        asyncExecutor.shutdown();
        asyncExecutor.awaitTermination(30, TimeUnit.SECONDS);
    }

    /**
     * Schedules a once off task to occur as soon as possible.
     * <p>
     * This task will be executed by the main server thread.
     *
     * @param task Task to be executed
     * @return Task id number (-1 if scheduling failed)
     */
    public int scheduleSyncDelayedTask(Runnable task) {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(authMe, task);
    }

    /**
     * Schedules a once off task to occur after a delay.
     * <p>
     * This task will be executed by the main server thread.
     *
     * @param task Task to be executed
     * @param delay Delay in server ticks before executing task
     * @return Task id number (-1 if scheduling failed)
     */
    public int scheduleSyncDelayedTask(Runnable task, long delay) {
        return Bukkit.getScheduler().scheduleSyncDelayedTask(authMe, task, delay);
    }

    /**
     * Schedules a synchronous task if async tasks are enabled; if not, it runs the task immediately.
     * Use this when {@link #runTaskOptionallyAsync(Runnable) optionally asynchronous tasks} have to
     * run something synchronously.
     *
     * @param task the task to be run
     */
    public void scheduleSyncTaskFromOptionallyAsyncTask(Runnable task) {
        if (useAsyncTasks) {
            scheduleSyncDelayedTask(task);
        } else {
            task.run();
        }
    }

    /**
     * Returns a task that will run on the next server tick.
     *
     * @param task the task to be run
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalArgumentException if task is null
     */
    public BukkitTask runTask(Runnable task) {
        return Bukkit.getScheduler().runTask(authMe, task);
    }

    /**
     * Returns a task that will run after the specified number of server
     * ticks.
     *
     * @param task the task to be run
     * @param delay the ticks to wait before running the task
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalArgumentException if task is null
     */
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
            runTaskAsynchronously(task);
        } else {
            task.run();
        }
    }

    /**
     * <b>Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will run asynchronously.
     *
     * @param task the task to be run
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalArgumentException if task is null
     */
    public void runTaskAsynchronously(Runnable task) {
        asyncExecutor.execute(task);
        //Bukkit.getScheduler().runTaskAsynchronously(authMe, task);
    }

    /**
     * <b>Asynchronous tasks should never access any API in Bukkit. Great care
     * should be taken to assure the thread-safety of asynchronous tasks.</b>
     * <p>
     * Returns a task that will repeatedly run asynchronously until cancelled,
     * starting after the specified number of server ticks.
     *
     * @param task the task to be run
     * @param delay the ticks to wait before running the task for the first
     *     time
     * @param period the ticks to wait between runs
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if task is null
     */
    public BukkitTask runTaskTimerAsynchronously(Runnable task, long delay, long period) {
        return Bukkit.getScheduler().runTaskTimerAsynchronously(authMe, task, delay, period);
    }

    /**
     * Schedules the given task to repeatedly run until cancelled, starting after the
     * specified number of server ticks.
     *
     * @param task the task to schedule
     * @param delay the ticks to wait before running the task
     * @param period the ticks to wait between runs
     * @return a BukkitTask that contains the id number
     * @throws IllegalArgumentException if plugin is null
     * @throws IllegalStateException if this was already scheduled
     * @see BukkitScheduler#runTaskTimer(Plugin, Runnable, long, long)
     */
    public BukkitTask runTaskTimer(BukkitRunnable task, long delay, long period) {
        return task.runTaskTimer(authMe, period, delay);
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
     * Safe way to retrieve the list of online players from the server. Depending on the
     * implementation of the server, either an array of {@link Player} instances is being returned,
     * or a Collection. Always use this wrapper to retrieve online players instead of {@link
     * Bukkit#getOnlinePlayers()} directly.
     *
     * @return collection of online players
     *
     * @see <a href="https://www.spigotmc.org/threads/solved-cant-use-new-getonlineplayers.33061/">SpigotMC
     * forum</a>
     * @see <a href="http://stackoverflow.com/questions/32130851/player-changed-from-array-to-collection">StackOverflow</a>
     */
    @SuppressWarnings("unchecked")
    public Collection<? extends Player> getOnlinePlayers() {
        if (getOnlinePlayersIsCollection) {
            return Bukkit.getOnlinePlayers();
        }
        try {
            // The lookup of a method via Reflections is rather expensive, so we keep a reference to it
            if (getOnlinePlayers == null) {
                getOnlinePlayers = Bukkit.class.getDeclaredMethod("getOnlinePlayers");
            }
            Object obj = getOnlinePlayers.invoke(null);
            if (obj instanceof Collection<?>) {
                return (Collection<? extends Player>) obj;
            } else if (obj instanceof Player[]) {
                return Arrays.asList((Player[]) obj);
            } else {
                String type = (obj == null) ? "null" : obj.getClass().getName();
                ConsoleLogger.warning("Unknown list of online players of type " + type);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            ConsoleLogger.logException("Could not retrieve list of online players:", e);
        }
        return Collections.emptyList();
    }

    /**
     * Calls an event with the given details.
     *
     * @param event Event details
     * @throws IllegalStateException Thrown when an asynchronous event is
     *     fired from synchronous code.
     */
    public void callEvent(Event event) {
        Bukkit.getPluginManager().callEvent(event);
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

    @Override
    public void reload(Settings settings) {
        useAsyncTasks = settings.getProperty(PluginSettings.USE_ASYNC_TASKS);
    }

    /**
     * Method run upon initialization to verify whether or not the Bukkit implementation
     * returns the online players as a Collection.
     *
     * @see #getOnlinePlayers()
     */
    private static boolean initializeOnlinePlayersIsCollectionField() {
        try {
            Method method = Bukkit.class.getDeclaredMethod("getOnlinePlayers");
            return method.getReturnType() == Collection.class;
        } catch (NoSuchMethodException e) {
            ConsoleLogger.warning("Error verifying if getOnlinePlayers is a collection! Method doesn't exist");
        }
        return false;
    }

    /**
     * Adds a ban to the this list. If a previous ban exists, this will
     * update the previous entry.
     *
     * @param ip the ip of the ban
     * @param reason reason for the ban, null indicates implementation default
     * @param expires date for the ban's expiration (unban), or null to imply
     *     forever
     * @param source source of the ban, null indicates implementation default
     * @return the entry for the newly created ban, or the entry for the
     *     (updated) previous ban
     */
    public BanEntry banIp(String ip, String reason, Date expires, String source) {
        return Bukkit.getServer().getBanList(BanList.Type.IP).addBan(ip, reason, expires, source);
    }

    /**
     * Dispatch a command as console
     *
     * @param command the command
     */
    public void dispatchConsoleCommand(String command) {
        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command);
    }
}
