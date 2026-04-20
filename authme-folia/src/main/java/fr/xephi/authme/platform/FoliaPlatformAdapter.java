package fr.xephi.authme.platform;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.listener.FoliaChatListener;
import fr.xephi.authme.listener.FoliaPlayerSpawnLocationListener;
import fr.xephi.authme.listener.PlayerOpenSignListener;
import fr.xephi.authme.service.CancellableTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.event.Listener;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;

/**
 * Platform adapter implementation for Folia 1.21.11+.
 * Reuses the Paper-derived API surface exposed by Folia while relying on
 * the core scheduler abstraction for region-safe task dispatch.
 */
public class FoliaPlatformAdapter extends AbstractPaperPlatformAdapter {

    private static final CancellableTask NO_OP_TASK = () -> { };

    @Override
    public boolean isOwnedByCurrentThread(Entity entity) {
        return Bukkit.isOwnedByCurrentRegion(entity);
    }

    @Override
    public boolean isGlobalThread() {
        return Bukkit.isGlobalTickThread();
    }

    @Override
    public void runOnEntityThread(AuthMe plugin, Entity entity, Runnable task) {
        ScheduledTask scheduledTask = entity.getScheduler().run(plugin, ignored -> task.run(), null);
        if (scheduledTask == null) {
            plugin.getLogger().fine("Skipped entity task because the entity scheduler was retired");
        }
    }

    @Override
    public CancellableTask runDelayedOnEntityThread(AuthMe plugin, Entity entity, Runnable task, long delay) {
        ScheduledTask scheduledTask = entity.getScheduler().runDelayed(plugin, ignored -> task.run(), null, delay);
        if (scheduledTask == null) {
            plugin.getLogger().fine("Skipped delayed entity task because the entity scheduler was retired");
            return NO_OP_TASK;
        }
        return scheduledTask::cancel;
    }

    @Override
    public CancellableTask runAtFixedRateOnEntityThread(AuthMe plugin, Entity entity, Runnable task,
                                                        long delay, long period) {
        ScheduledTask scheduledTask = entity.getScheduler().runAtFixedRate(plugin, ignored -> task.run(), null,
            delay, period);
        if (scheduledTask == null) {
            plugin.getLogger().fine("Skipped repeating entity task because the entity scheduler was retired");
            return NO_OP_TASK;
        }
        return scheduledTask::cancel;
    }

    @Override
    public void runOnGlobalThread(AuthMe plugin, Runnable task) {
        Bukkit.getGlobalRegionScheduler().run(plugin, ignored -> task.run());
    }

    @Override
    public CancellableTask runDelayedOnGlobalThread(AuthMe plugin, Runnable task, long delay) {
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, ignored -> task.run(), delay)::cancel;
    }

    @Override
    public String getPlatformName() {
        return "folia-1.21";
    }

    @Override
    public String getCompatibilityError() {
        return getCompatibilityError("This AuthMe Folia build requires the Folia 1.21.11+ API.",
            "io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler",
            "io.papermc.paper.event.player.AsyncChatEvent",
            "io.papermc.paper.event.player.AsyncPlayerSpawnLocationEvent",
            "io.papermc.paper.event.player.PlayerOpenSignEvent");
    }

    @Override
    public List<Class<? extends Listener>> getAdditionalListeners() {
        return Arrays.asList(FoliaChatListener.class, FoliaPlayerSpawnLocationListener.class, PlayerOpenSignListener.class);
    }
}
