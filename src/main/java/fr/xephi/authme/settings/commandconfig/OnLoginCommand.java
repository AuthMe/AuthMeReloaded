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

    /**
     * Constructor.
     *
     * @param command the command to execute
     * @param executor the executor of the command
     * @param numberOfOtherAccountsAtLeast required number of accounts for the command to run
     * @param numberOfOtherAccountsLessThan max threshold of accounts, from which the command will not be run
     */
    public OnLoginCommand(String command, Executor executor, Optional<Integer> numberOfOtherAccountsAtLeast,
                          Optional<Integer> numberOfOtherAccountsLessThan) {
        super(command, executor);
        this.numberOfOtherAccountsAtLeast = numberOfOtherAccountsAtLeast;
        this.numberOfOtherAccountsLessThan = numberOfOtherAccountsLessThan;
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
