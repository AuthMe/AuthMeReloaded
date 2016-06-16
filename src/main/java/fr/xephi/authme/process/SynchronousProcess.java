package fr.xephi.authme.process;

/**
 * Marker interface for synchronous processes.
 * <p>
 * Such processes are scheduled by {@link AsynchronousProcess asynchronous tasks} to perform tasks
 * which are required to be executed synchronously (e.g. interactions with the Bukkit API).
 */
public interface SynchronousProcess {
}
