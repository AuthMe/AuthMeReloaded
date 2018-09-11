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
     * Creates a copy of this Command object, setting the given command text on the copy.
     *
     * @param command the command text to use in the copy
     * @return copy of the source with the new command
     */
    public Command copyWithCommand(String command) {
        Command copy = new Command();
        setValuesToCopyWithNewCommand(copy, command);
        return copy;
    }

    protected void setValuesToCopyWithNewCommand(Command copy, String newCommand) {
        copy.command = newCommand;
        copy.executor = this.executor;
        copy.delay = this.delay;
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
