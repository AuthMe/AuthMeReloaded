package fr.xephi.authme.process;

import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;

public class AsyncUserScheduler {

    @Inject
    private BukkitService bukkitService;

    private KeyedSequentialScheduler<String> asyncUserScheduler;

    AsyncUserScheduler() {
        this.asyncUserScheduler = new KeyedSequentialScheduler<>(runnable ->
            bukkitService.runTaskAsynchronously(runnable));
    }

    public void runTask(String playerName, Runnable runnable) {
        if (bukkitService.isUseAsyncTasks()) {
            asyncUserScheduler.submit(playerName.toLowerCase(), runnable);
        } else {
            runnable.run();
        }
    }

    public void runTask(Player player, Runnable runnable) {
        runTask(player.getName(), runnable);
    }

    public class KeyedSequentialScheduler<K> {
        private Map<K, SequentialExecutor> executors;
        private Function<Runnable, BukkitTask> scheduler;

        public KeyedSequentialScheduler(Function<Runnable, BukkitTask> scheduler) {
            this.executors = new LinkedHashMap<>();
            this.scheduler = scheduler;
        }

        public void submit(K key, Runnable runnable) {
            SequentialExecutor executor = executors.get(key);
            if (executor == null) {
                executor = new SequentialExecutor(scheduler, () -> executors.remove(key));
                executors.put(key, executor);
            }
            executor.submit(runnable);
        }
    }

    public class SequentialExecutor {
        private Queue<Runnable> queue;
        private Function<Runnable, BukkitTask> scheduler;
        private Runnable callback;

        private BukkitTask executor;

        public SequentialExecutor(Function<Runnable, BukkitTask> scheduler, Runnable callback) {
            this.queue = new LinkedBlockingQueue<>();
            this.scheduler = scheduler;
            this.callback = callback;
        }

        public void submit(Runnable task) {
            queue.add(task);
            if (executor == null || executor.isCancelled()) {
                executor = scheduler.apply(() -> {
                    while (!queue.isEmpty()) {
                        queue.poll().run();
                    }
                    callback.run();
                });
            }
        }
    }
}
