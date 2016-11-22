package fr.xephi.authme.settings.commandconfig;

/**
 * Command to be run.
 */
public class Command {

    /** The command to execute. */
    private String command;
    /** The executor of the command. */
    private Executor executor = Executor.PLAYER;

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
        this.command = command;
        this.executor = executor;
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

    @Override
    public String toString() {
        return command + " (" + executor + ")";
    }
}
