package fr.xephi.authme.platform;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.listener.LegacyPlayerLoginListener;
import fr.xephi.authme.listener.LegacyPlayerSpawnLocationListener;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.listener.packetevents.PacketEventsListenerRegistry;
import fr.xephi.authme.service.CancellableTask;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PremiumLoginVerifier;
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

    // Kept lazy so PacketEvents-dependent classes are only loaded after PacketEvents has been confirmed present.
    private PacketInterceptionAdapter packetInterceptionAdapter;

    @Override
    public void registerInventoryProtection(PlayerCache playerCache, DataSource dataSource) {
        getOrCreatePacketInterceptionAdapter().registerInventoryProtection(playerCache, dataSource);
    }

    @Override
    public void unregisterInventoryProtection() {
        if (packetInterceptionAdapter != null) {
            packetInterceptionAdapter.unregisterInventoryProtection();
        }
    }

    @Override
    public void sendBlankInventoryPacket(Player player) {
        if (packetInterceptionAdapter != null) {
            packetInterceptionAdapter.sendBlankInventoryPacket(player);
        }
    }

    @Override
    public void registerTabCompleteBlock(PlayerCache playerCache) {
        getOrCreatePacketInterceptionAdapter().registerTabCompleteBlock(playerCache);
    }

    @Override
    public void unregisterTabCompleteBlock() {
        if (packetInterceptionAdapter != null) {
            packetInterceptionAdapter.unregisterTabCompleteBlock();
        }
    }

    @Override
    public void registerPremiumVerification(DataSource dataSource, PremiumLoginVerifier verifier,
                                            PendingPremiumCache pendingPremiumCache) {
        getOrCreatePacketInterceptionAdapter()
            .registerPremiumVerification(dataSource, verifier, pendingPremiumCache);
    }

    @Override
    public void unregisterPremiumVerification() {
        if (packetInterceptionAdapter != null) {
            packetInterceptionAdapter.unregisterPremiumVerification();
        }
    }

    @Override
    public boolean isProxyForwardingEnabled() {
        try {
            return Bukkit.getServer().spigot().getConfig()
                .getBoolean("settings.bungeecord", false);
        } catch (Exception ignored) {
            return false;
        }
    }

    protected PacketInterceptionAdapter createPacketInterceptionAdapter() {
        return new PacketEventsListenerRegistry();
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

    private PacketInterceptionAdapter getOrCreatePacketInterceptionAdapter() {
        if (packetInterceptionAdapter == null) {
            packetInterceptionAdapter = createPacketInterceptionAdapter();
        }
        return packetInterceptionAdapter;
    }
}
