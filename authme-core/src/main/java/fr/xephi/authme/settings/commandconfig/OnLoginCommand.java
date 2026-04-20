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
     * Creates a copy of this object, using the given command as new {@link Command#command command}.
     *
     * @param command the command text to use in the copy
     * @return copy of the source with the new command
     */
    @Override
    public OnLoginCommand copyWithCommand(String command) {
        OnLoginCommand copy = new OnLoginCommand();
        setValuesToCopyWithNewCommand(copy, command);
        copy.ifNumberOfAccountsAtLeast = this.ifNumberOfAccountsAtLeast;
        copy.ifNumberOfAccountsLessThan = this.ifNumberOfAccountsLessThan;
        return copy;
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
