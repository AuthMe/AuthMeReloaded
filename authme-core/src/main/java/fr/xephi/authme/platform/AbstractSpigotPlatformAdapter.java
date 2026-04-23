package fr.xephi.authme.platform;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.listener.LegacyPlayerLoginListener;
import fr.xephi.authme.listener.LegacyPlayerSpawnLocationListener;
import fr.xephi.authme.service.CancellableTask;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

/**
 * Base implementation of {@link PlatformAdapter} for all Spigot versions.
 * Uses synchronous (blocking) teleport via the Bukkit API.
 */
public abstract class AbstractSpigotPlatformAdapter implements PlatformAdapter {

    @Override
    public List<Class<? extends Listener>> getListeners() {
        return EventRegistrationAdapter.combineListeners(
            EventRegistrationAdapter.getCommonListeners(),
            List.of(LegacyPlayerLoginListener.class, LegacyPlayerSpawnLocationListener.class));
    }

    @Override
    public void teleportPlayer(Player player, Location location) {
        player.teleport(location);
    }

    @Override
    public Location getPlayerRespawnLocation(Player player) {
        return player.getBedSpawnLocation();
    }

    @Override
    public boolean isOwnedByCurrentThread(Entity entity) {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public boolean isGlobalThread() {
        return Bukkit.isPrimaryThread();
    }

    @Override
    public void runOnEntityThread(AuthMe plugin, Entity entity, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public CancellableTask runDelayedOnEntityThread(AuthMe plugin, Entity entity, Runnable task, long delay) {
        return wrapTask(Bukkit.getScheduler().runTaskLater(plugin, task, delay));
    }

    @Override
    public CancellableTask runAtFixedRateOnEntityThread(AuthMe plugin, Entity entity, Runnable task,
                                                        long delay, long period) {
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        };
        return wrapTask(bukkitRunnable.runTaskTimer(plugin, delay, period));
    }

    @Override
    public CancellableTask runAsyncTask(AuthMe plugin, Runnable task) {
        return wrapTask(Bukkit.getScheduler().runTaskAsynchronously(plugin, task));
    }

    @Override
    public CancellableTask runAsyncTaskTimer(AuthMe plugin, Runnable task, long delay, long period) {
        BukkitRunnable bukkitRunnable = new BukkitRunnable() {
            @Override
            public void run() {
                task.run();
            }
        };
        return wrapTask(bukkitRunnable.runTaskTimerAsynchronously(plugin, delay, period));
    }

    @Override
    public void runOnGlobalThread(AuthMe plugin, Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }

    @Override
    public CancellableTask runDelayedOnGlobalThread(AuthMe plugin, Runnable task, long delay) {
        return wrapTask(Bukkit.getScheduler().runTaskLater(plugin, task, delay));
    }

    protected final String getCompatibilityError(String errorMessage, String... requiredClasses) {
        for (String className : requiredClasses) {
            if (!Utils.isClassLoaded(className)) {
                return errorMessage;
            }
        }
        return null;
    }

    private static CancellableTask wrapTask(BukkitTask task) {
        return task::cancel;
    }
}
