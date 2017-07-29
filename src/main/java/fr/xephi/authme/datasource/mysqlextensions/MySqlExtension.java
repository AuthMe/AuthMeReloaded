package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalInt;

/**
 * Extension for the MySQL data source for forums. For certain password hashes (e.g. phpBB), we want
 * to hook into the forum board and execute some actions specific to the forum software.
 */
public abstract class MySqlExtension {

    protected final Columns col;
    protected final String tableName;

    MySqlExtension(Settings settings, Columns col) {
        this.col = col;
        this.tableName = settings.getProperty(DatabaseSettings.MYSQL_TABLE);
    }

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

    /**
     * Fetches the database ID of the given name from the database.
     *
     * @param name the name to get the ID for
     * @param con connection to the sql table
     * @return id of the playerAuth, or empty OptionalInt if the name is not registered
     * @throws SQLException .
     */
    protected OptionalInt retrieveIdFromTable(String name, Connection con) throws SQLException {
        String sql = "SELECT " + col.ID + " FROM " + tableName + " WHERE " + col.NAME + "=?;";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, name);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return OptionalInt.of(rs.getInt(col.ID));
                }
            }
        }
        return OptionalInt.empty();
    }
}
