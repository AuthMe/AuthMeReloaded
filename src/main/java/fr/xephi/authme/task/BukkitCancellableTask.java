package fr.xephi.authme.task;

import org.bukkit.scheduler.BukkitTask;

import java.util.function.Consumer;

public class BukkitCancellableTask implements CancellableTask {

    private final BukkitTask bukkitTask;

    public BukkitCancellableTask(BukkitTask bukkitTask) {
        this.bukkitTask = bukkitTask;
    }

    public static Consumer<BukkitTask> mapConsumer(Consumer<CancellableTask> task) {
        return c -> task.accept(new BukkitCancellableTask(c));
    }

    @Override
    public void cancel() {
        bukkitTask.cancel();
    }

    @Override
    public boolean isCancelled() {
        return bukkitTask.isCancelled();
    }
}
