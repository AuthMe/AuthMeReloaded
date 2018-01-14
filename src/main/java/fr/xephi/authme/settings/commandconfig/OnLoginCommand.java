package fr.xephi.authme.settings.commandconfig;

import java.util.Optional;

/**
 * Configurable command for when a player logs in.
 */
public class OnLoginCommand extends Command {

    private Optional<Integer> numberOfOtherAccountsAtLeast;
    private Optional<Integer> numberOfOtherAccountsLessThan;

    /**
     * Default constructor (for bean mapping).
     */
    public OnLoginCommand() {
    }

    /**
     * Constructor.
     *
     * @param command the command to execute
     * @param executor the executor of the command
     */
    public OnLoginCommand(String command, Executor executor) {
        super(command, executor);
    }

    public Optional<Integer> getNumberOfOtherAccountsAtLeast() {
        return numberOfOtherAccountsAtLeast;
    }

    public void setNumberOfOtherAccountsAtLeast(Optional<Integer> numberOfOtherAccountsAtLeast) {
        this.numberOfOtherAccountsAtLeast = numberOfOtherAccountsAtLeast;
    }

    public Optional<Integer> getNumberOfOtherAccountsLessThan() {
        return numberOfOtherAccountsLessThan;
    }

    public void setNumberOfOtherAccountsLessThan(Optional<Integer> numberOfOtherAccountsLessThan) {
        this.numberOfOtherAccountsLessThan = numberOfOtherAccountsLessThan;
    }
}
