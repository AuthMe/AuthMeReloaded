package fr.xephi.authme.settings.commandconfig;

/**
 * Command to be run.
 */
public class Command {

    /** The command to execute. */
    private String command;
    /** The executor of the command. */
    private Executor executor = Executor.PLAYER;
    /** Delay before executing the command (in ticks) */
    private long delay = 0;

    /**
     * Default constructor (for bean mapping).
     */
    public Command() {
    }

    /**
     * Constructor.
     *
     * @param command the command
     * @param executor the executor of the command
     */
    public Command(String command, Executor executor) {
        this(command, executor, 0);
    }

    /**
     * Constructor.
     *
     * @param command the command
     * @param executor the executor of the command
     * @param delay the delay (in ticks) before executing command
     */
    public Command(String command, Executor executor, long delay) {
        this.command = command;
        this.executor = executor;
        this.delay = delay;
    }

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public Executor getExecutor() {
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    @Override
    public String toString() {
        return command + " (" + executor + ")";
    }
}
