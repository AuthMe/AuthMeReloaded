package fr.xephi.authme.datasource;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.security.crypts.HashedPassword;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface for manipulating {@link PlayerAuth} objects from a data source.
 */
public interface DataSource extends Reloadable {

    /**
     * Return whether there is a record for the given username.
     *
     * @param user The username to look up
     * @return True if there is a record, false otherwise
     */
    boolean isAuthAvailable(String user);

    /**
     * Return the hashed password of the player.
     *
     * @param user The user whose password should be retrieve
     * @return The password hash of the player
     */
    HashedPassword getPassword(String user);

    /**
     * Retrieve the entire PlayerAuth object associated with the username.
     *
     * @param user The user to retrieve
     * @return The PlayerAuth object for the given username
     */
    PlayerAuth getAuth(String user);

    /**
     * Save a new PlayerAuth object.
     *
     * @param auth The new PlayerAuth to persist
     * @return True upon success, false upon failure
     */
    boolean saveAuth(PlayerAuth auth);

    /**
     * Update the session of a record (IP, last login, real name).
     *
     * @param auth The PlayerAuth object to update in the database
     * @return True upon success, false upon failure
     */
    boolean updateSession(PlayerAuth auth);

    /**
     * Update the password of the given PlayerAuth object.
     *
     * @param auth The PlayerAuth whose password should be updated
     * @return True upon success, false upon failure
     */
    boolean updatePassword(PlayerAuth auth);

    /**
     * Update the password of the given player.
     *
     * @param user The user whose password should be updated
     * @param password The new password
     * @return True upon success, false upon failure
     */
    boolean updatePassword(String user, HashedPassword password);

    /**
     * Get all records in the database whose last login was before the given time.
     *
     * @param until The minimum last login
     * @param includeEntriesWithLastLoginZero Whether entries with lastlogin = 0 should be included or not,
     *        see <a href="https://github.com/Xephi/AuthMeReloaded/issues/886">issue #886</a>
     * @return The account names selected to purge
     */
    Set<String> getRecordsToPurge(long until, boolean includeEntriesWithLastLoginZero);

    /**
     * Purge the given players from the database.
     *
     * @param toPurge The players to purge
     */
    void purgeRecords(Collection<String> toPurge);

    /**
     * Remove a user record from the database.
     *
     * @param user The user to remove
     * @return True upon success, false upon failure
     */
    boolean removeAuth(String user);

    /**
     * Update the quit location of a PlayerAuth.
     *
     * @param auth The entry whose quit location should be updated
     * @return True upon success, false upon failure
     */
    boolean updateQuitLoc(PlayerAuth auth);

    /**
     * Return all usernames associated with the given IP address.
     *
     * @param ip The IP address to look up
     * @return Usernames associated with the given IP address
     */
    List<String> getAllAuthsByIp(String ip);

    /**
     * Return the number of accounts associated with the given email address.
     *
     * @param email The email address to look up
     * @return Number of accounts using the given email address
     */
    int countAuthsByEmail(String email);

    /**
     * Update the email of the PlayerAuth in the data source.
     *
     * @param auth The PlayerAuth whose email should be updated
     * @return True upon success, false upon failure
     */
    boolean updateEmail(PlayerAuth auth);

    /**
     * Close the underlying connections to the data source.
     */
    void close();

    /**
     * Return the data source type.
     *
     * @return the data source type
     */
    DataSourceType getType();

    /**
     * Query the datasource whether the player is logged in or not.
     *
     * @param user The name of the player to verify
     * @return True if logged in, false otherwise
     */
    boolean isLogged(String user);

    /**
     * Set a player as logged in.
     *
     * @param user The name of the player to change
     */
    void setLogged(String user);

    /**
     * Set a player as unlogged (not logged in).
     *
     * @param user The name of the player to change
     */
    void setUnlogged(String user);

    /**
     * Set all players who are marked as logged in as NOT logged in.
     */
    void purgeLogged();

    /**
     * Return all players which are logged in.
     *
     * @return All logged in players
     */
    List<PlayerAuth> getLoggedPlayers();

    /**
     * Return the number of registered accounts.
     *
     * @return Total number of accounts
     */
    int getAccountsRegistered();

    /**
     * Update a player's real name (capitalization).
     *
     * @param user The name of the user (lowercase)
     * @param realName The real name of the user (proper casing)
     * @return True upon success, false upon failure
     */
    boolean updateRealName(String user, String realName);

    /**
     * Return all players of the database.
     *
     * @return List of all players
     */
    List<PlayerAuth> getAllAuths();

    /**
     * Reload the data source.
     */
    @Override
    void reload();

}
