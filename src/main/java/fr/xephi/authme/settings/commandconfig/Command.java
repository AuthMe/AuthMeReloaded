package fr.xephi.authme.settings.commandconfig;

/**
 * Command to be run.
 */
public class Command {

    /** The command to execute. */
    private String command;
    /** The executor of the command. */
    private Executor executor = Executor.PLAYER;

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
}
