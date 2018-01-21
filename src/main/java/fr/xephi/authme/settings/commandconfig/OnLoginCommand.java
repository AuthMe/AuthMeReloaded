package fr.xephi.authme.settings.commandconfig;

import java.util.Optional;

/**
 * Configurable command for when a player logs in.
 */
public class OnLoginCommand extends Command {

    private Optional<Integer> ifNumberOfAccountsAtLeast = Optional.empty();
    private Optional<Integer> ifNumberOfAccountsLessThan = Optional.empty();

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
     * @param ifNumberOfAccountsAtLeast required number of accounts for the command to run
     * @param ifNumberOfAccountsLessThan max threshold of accounts, from which the command will not be run
     */
    public OnLoginCommand(String command, Executor executor, Optional<Integer> ifNumberOfAccountsAtLeast,
                          Optional<Integer> ifNumberOfAccountsLessThan) {
        super(command, executor);
        this.ifNumberOfAccountsAtLeast = ifNumberOfAccountsAtLeast;
        this.ifNumberOfAccountsLessThan = ifNumberOfAccountsLessThan;
    }

    public Optional<Integer> getIfNumberOfAccountsAtLeast() {
        return ifNumberOfAccountsAtLeast;
    }

    public void setIfNumberOfAccountsAtLeast(Optional<Integer> ifNumberOfAccountsAtLeast) {
        this.ifNumberOfAccountsAtLeast = ifNumberOfAccountsAtLeast;
    }

    public Optional<Integer> getIfNumberOfAccountsLessThan() {
        return ifNumberOfAccountsLessThan;
    }

    public void setIfNumberOfAccountsLessThan(Optional<Integer> ifNumberOfAccountsLessThan) {
        this.ifNumberOfAccountsLessThan = ifNumberOfAccountsLessThan;
    }
}
