package fr.xephi.authme.platform;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.listener.LegacyPlayerLoginListener;
import fr.xephi.authme.listener.LegacyPlayerPickupItemListener;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.listener.packetevents.PacketEventsListenerRegistry;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CancellableTask;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PremiumLoginVerifier;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Base implementation of {@link PlatformAdapter} for all Spigot versions.
 * Uses synchronous (blocking) teleport via the Bukkit API.
 */
public abstract class AbstractSpigotPlatformAdapter implements PlatformAdapter {

    private static final Method PLAYER_GET_LOCALE_METHOD;
    private static final Method PLAYER_SPIGOT_METHOD;
    private static final Method SPIGOT_GET_LOCALE_METHOD;
    private static final Method BLOCK_IS_PASSABLE_METHOD;

    static {
        Method direct = null;
        Method spigot = null;
        Method spigotLocale = null;
        Method passable = null;
        try {
            direct = Player.class.getMethod("getLocale");
        } catch (NoSuchMethodException ignored) {
            // direct Player#getLocale not available (pre-1.12)
        }
        if (direct == null) {
            try {
                spigot = Player.class.getMethod("spigot");
                spigotLocale = spigot.getReturnType().getMethod("getLocale");
            } catch (Exception ignored) {
                spigot = null;
                spigotLocale = null;
            }
        }
        try {
            passable = Block.class.getMethod("isPassable");
        } catch (NoSuchMethodException ignored) {
            // Block#isPassable not available (pre-1.13)
        }
        PLAYER_GET_LOCALE_METHOD = direct;
        PLAYER_SPIGOT_METHOD = spigot;
        SPIGOT_GET_LOCALE_METHOD = spigotLocale;
        BLOCK_IS_PASSABLE_METHOD = passable;
    }

    @Override
    public Optional<String> getPlayerLocale(Player player) {
        try {
            String locale = null;
            if (PLAYER_GET_LOCALE_METHOD != null) {
                Object result = PLAYER_GET_LOCALE_METHOD.invoke(player);
                if (result instanceof String) {
                    locale = (String) result;
                }
            } else if (PLAYER_SPIGOT_METHOD != null && SPIGOT_GET_LOCALE_METHOD != null) {
                Object spigot = PLAYER_SPIGOT_METHOD.invoke(player);
                if (spigot != null) {
                    Object result = SPIGOT_GET_LOCALE_METHOD.invoke(spigot);
                    if (result instanceof String) {
                        locale = (String) result;
                    }
                }
            }
            if (locale == null || locale.trim().isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(locale);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    @Override
    public int getSpawnRadius(World world) {
        try {
            String value = world.getGameRuleValue("spawnRadius");
            if (value == null) {
                return 0;
            }
            int radius = Integer.parseInt(value);
            if (radius <= 0) {
                return 0;
            }
            return radius;
        } catch (Exception ignored) {
            return 0;
        }
    }

    @Override
    public boolean isBlockPassable(Block block) {
        if (BLOCK_IS_PASSABLE_METHOD != null) {
            try {
                return (Boolean) BLOCK_IS_PASSABLE_METHOD.invoke(block);
            } catch (Exception ignored) {
                // fallback to material solidity check
            }
        }
        return !block.getType().isSolid();
    }

    protected boolean isClassAvailable(String className) {
        return Utils.isClassLoaded(className);
    }

    @Override
    public List<Class<? extends Listener>> getListeners() {
        List<Class<? extends Listener>> listeners = new ArrayList<>(EventRegistrationAdapter.getCommonListeners());
        listeners.add(LegacyPlayerLoginListener.class);

        boolean hasModernPickup = isClassAvailable("org.bukkit.event.entity.EntityPickupItemEvent");
        if (!hasModernPickup) {
            listeners.add(LegacyPlayerPickupItemListener.class);
        }

        String[][] optionalListeners = {
            {"org.spigotmc.event.player.PlayerSpawnLocationEvent", "fr.xephi.authme.listener.LegacyPlayerSpawnLocationListener"},
            {"org.bukkit.event.player.PlayerSwapHandItemsEvent", "fr.xephi.authme.listener.PlayerSwapHandItemsListener"},
            {"org.bukkit.event.entity.EntityAirChangeEvent", "fr.xephi.authme.listener.EntityAirChangeListener"},
            {"org.bukkit.event.entity.EntityPickupItemEvent", "fr.xephi.authme.listener.EntityPickupItemListener"}
        };
        for (String[] entry : optionalListeners) {
            String apiClass = entry[0];
            String listenerClass = entry[1];
            if (isClassAvailable(apiClass)) {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends Listener> clazz = (Class<? extends Listener>)
                        Class.forName(listenerClass, false, getClass().getClassLoader()).asSubclass(Listener.class);
                    listeners.add(clazz);
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Missing listener class: " + listenerClass, e);
                }
            }
        }
        return Collections.unmodifiableList(new ArrayList<>(listeners));
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
                                            PendingPremiumCache pendingPremiumCache, BukkitService bukkitService) {
        getOrCreatePacketInterceptionAdapter()
            .registerPremiumVerification(dataSource, verifier, pendingPremiumCache, bukkitService);
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
        } catch (LinkageError ignored) {
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
