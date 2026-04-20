package fr.xephi.authme.initialization;

/**
 * Interface for reloadable entities.
 *
 * @see fr.xephi.authme.command.executable.authme.ReloadCommand
 */
public interface Reloadable {

    /**
     * Performs the reload action.
     */
    void reload();

}
