package fr.xephi.authme.task;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.function.Consumer;

public class FoliaCancellableTask implements CancellableTask {

    private final ScheduledTask scheduledTask;

    public FoliaCancellableTask(ScheduledTask scheduledTask) {
        this.scheduledTask = scheduledTask;
    }

    public static CancellableTask mapTask(ScheduledTask task) {
        return task != null ? new FoliaCancellableTask(task) : null;
    }

    public static Consumer<ScheduledTask> mapConsumer(Consumer<CancellableTask> task) {
        return c -> task.accept(new FoliaCancellableTask(c));
    }

    @Override
    public void cancel() {
        scheduledTask.cancel();
    }

    @Override
    public boolean isCancelled() {
        return scheduledTask.isCancelled();
    }
}
