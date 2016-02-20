package fr.xephi.authme.datasource;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;

import java.util.List;

/**
 * Interface for manipulating {@link PlayerAuth} objects from a data source.
 */
public interface DataSource {

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
     * Purge all records in the database whose last login was longer ago than
     * the given time.
     *
     * @param until The minimum last login
     * @return The account names that have been removed
     */
    List<String> autoPurgeDatabase(long until);

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
     * Return all usernames associated with the given email address.
     *
     * @param email The email address to look up
     * @return Users using the given email address
     */
    List<String> getAllAuthsByEmail(String email);

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

    void reload();

    /**
     * Method purgeBanned.
     *
     * @param banned List of String
     */
    void purgeBanned(List<String> banned);

    /**
     * Method getType.
     *
     * @return DataSourceType
     */
    DataSourceType getType();

    /**
     * Method isLogged.
     *
     * @param user String
     *
     * @return boolean
     */
    boolean isLogged(String user);

    /**
     * Method setLogged.
     *
     * @param user String
     */
    void setLogged(String user);

    /**
     * Method setUnlogged.
     *
     * @param user String
     */
    void setUnlogged(String user);

    void purgeLogged();

    /**
     * Method getAccountsRegistered.
     *
     * @return int
     */
    int getAccountsRegistered();

    /**
     * Method updateName.
     *
     * @param oldOne String
     * @param newOne String
     */
    void updateName(String oldOne, String newOne);

    boolean updateRealName(String user, String realName);

    boolean updateIp(String user, String ip);

    /**
     * Method getAllAuths.
     *
     * @return List of PlayerAuth
     */
    List<PlayerAuth> getAllAuths();

    /**
     * Method getLoggedPlayers.
     *
     * @return List of PlayerAuth
     */
    List<PlayerAuth> getLoggedPlayers();

    boolean isEmailStored(String email);

}
