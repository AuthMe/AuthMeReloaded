package fr.xephi.authme.task;

import org.jetbrains.annotations.NotNull;

public interface CancellableTask {

    /**
     * Attempts to cancel this task, returning the result of the attempt. In all cases, if the task is currently
     * being executed no attempt is made to halt the task, however any executions in the future are halted.
     */
    void cancel();

    /**
     * Check if the task has been cancelled
     */
    boolean isCancelled();
}
