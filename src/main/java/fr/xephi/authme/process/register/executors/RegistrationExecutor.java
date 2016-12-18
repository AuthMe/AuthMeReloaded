package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.data.auth.PlayerAuth;

/**
 * Performs the registration action.
 */
public interface RegistrationExecutor {

    /**
     * Returns whether the registration may take place. Use this method to execute
     * checks specific to the registration method.
     * <p>
     * If this method returns {@code false}, it is expected that the executor inform
     * the player about the error within this method call.
     *
     * @return true if registration may be performed, false otherwise
     */
    boolean isRegistrationAdmitted();

    /**
     * Constructs the PlayerAuth object to persist into the database.
     *
     * @return the player auth to register in the data source
     */
    PlayerAuth buildPlayerAuth();

    /**
     * Follow-up method called after the player auth could be added into the database.
     */
    void executePostPersistAction();

}
