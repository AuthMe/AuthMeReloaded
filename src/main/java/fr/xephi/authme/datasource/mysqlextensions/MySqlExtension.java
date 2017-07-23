package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Extension for the MySQL data source for forums. For certain password hashes (e.g. phpBB), we want
 * to hook into the forum board and execute some actions specific to the forum software.
 */
public abstract class MySqlExtension {

    /**
     * Performs additional actions when a new player is saved.
     *
     * @param auth the player auth that has been saved
     * @param con connection to the sql table
     * @throws SQLException .
     */
    public void saveAuth(PlayerAuth auth, Connection con) throws SQLException {
        // extend for custom behavior
    }

    /**
     * Writes properties to the given PlayerAuth object that need to be retrieved in a specific manner
     * when a PlayerAuth object is read from the table.
     *
     * @param auth the player auth object to extend
     * @param id the database id of the player auth entry
     * @param con connection to the sql table
     * @throws SQLException .
     */
    public void extendAuth(PlayerAuth auth, int id, Connection con) throws SQLException {
        // extend for custom behavior
    }

    /**
     * Performs additional actions when a user's password is changed.
     *
     * @param user the name of the player (lowercase)
     * @param password the new password to set
     * @param con connection to the sql table
     * @throws SQLException .
     */
    public void changePassword(String user, HashedPassword password, Connection con) throws SQLException {
        // extend for custom behavior
    }

    /**
     * Performs additional actions when a player is removed from the database.
     *
     * @param user the user to remove
     * @param con connection to the sql table
     * @throws SQLException .
     */
    public void removeAuth(String user, Connection con) throws SQLException {
        // extend for custom behavior
    }

}
