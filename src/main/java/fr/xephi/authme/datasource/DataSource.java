package fr.xephi.authme.datasource;

import ch.jalu.datasourcecolumns.data.DataSourceValue;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.player.NamedIdentifier;
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
     * Return whether the data source is cached and needs to send plugin messaging updates.
     *
     * @return true if the data source is cached.
     */
    default boolean isCached() {
        return false;
    }

    /**
     * Return whether there is a record for the given username.
     *
     * @param identifier The identifier of the user to look up
     * @return True if there is a record, false otherwise
     */
    boolean isAuthAvailable(NamedIdentifier identifier);

    /**
     * Return the hashed password of the player.
     *
     * @param identifier The identifier of the user whose password should be retrieve
     * @return The password hash of the player
     */
    HashedPassword getPassword(NamedIdentifier identifier);

    /**
     * Retrieve the entire PlayerAuth object associated with the username.
     *
     * @param identifier The identifier of the user  to retrieve
     * @return The PlayerAuth object for the given username
     */
    PlayerAuth getAuth(NamedIdentifier identifier);

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
     * @param identifier The identifier of the user whose password should be updated
     * @param password The new password
     * @return True upon success, false upon failure
     */
    boolean updatePassword(NamedIdentifier identifier, HashedPassword password);

    /**
     * Get all records in the database whose last login was before the given time.
     *
     * @param until The minimum last login
     * @return The account names selected to purge
     */
    Set<String> getRecordsToPurge(long until);

    /**
     * Purge the given players from the database.
     *
     * @param toPurge The players to purge
     */
    void purgeRecords(Collection<NamedIdentifier> toPurge);

    /**
     * Remove a user record from the database.
     *
     * @param identifier The identifier of the user to remove
     * @return True upon success, false upon failure
     */
    boolean removeAuth(NamedIdentifier identifier);

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
    List<NamedIdentifier> getAllAuthsByIp(String ip);

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
    void closeConnection();

    /**
     * Return the data source type.
     *
     * @return the data source type
     */
    DataSourceType getType();

    /**
     * Query the datasource whether the player is logged in or not.
     *
     * @param identifier The identifier of the the player to verify
     * @return True if logged in, false otherwise
     */
    boolean isLogged(NamedIdentifier identifier);

    /**
     * Set a player as logged in.
     *
     * @param identifier The identifier of the player to change
     */
    void setLogged(NamedIdentifier identifier);

    /**
     * Set a player as unlogged (not logged in).
     *
     * @param identifier identifier of the player to change
     */
    void setUnlogged(NamedIdentifier identifier);

    /**
     * Query the datasource whether the player has an active session or not.
     * Warning: this value won't expire, you have also to check the user's last login timestamp.
     *
     * @param identifier The identifier of the player to verify
     * @return True if the user has a valid session, false otherwise
     */
    boolean hasSession(NamedIdentifier identifier);

    /**
     * Mark the user's hasSession value to true.
     *
     * @param identifier The identifier of the player to change
     */
    void grantSession(NamedIdentifier identifier);

    /**
     * Mark the user's hasSession value to false.
     *
     * @param identifier The identifier of the player to change
     */
    void revokeSession(NamedIdentifier identifier);

    /**
     * Set all players who are marked as logged in as NOT logged in.
     */
    void purgeLogged();

    /**
     * Return all players which are logged in and whose email is not set.
     *
     * @return logged in players with no email
     */
    List<NamedIdentifier> getLoggedPlayersWithEmptyMail();

    /**
     * Return the number of registered accounts.
     *
     * @return Total number of accounts
     */
    int getAccountsRegistered();

    /**
     * Update a player's real name (capitalization).
     *
     * @param identifier The identifier of the user
     * @return True upon success, false upon failure
     */
    boolean updateRealName(NamedIdentifier identifier);

    /**
     * Returns the email of the user.
     *
     * @param identifier the identifier of the user to retrieve an email for
     * @return the email saved for the user, or null if user or email is not present
     */
    DataSourceValue<String> getEmail(NamedIdentifier identifier);

    /**
     * Return all players of the database.
     *
     * @return List of all players
     */
    List<PlayerAuth> getAllAuths();

    /**
     * Returns the last ten players who have recently logged in (first ten players with highest last login date).
     *
     * @return the 10 last players who last logged in
     */
    List<PlayerAuth> getRecentlyLoggedInPlayers();

    /**
     * Sets the given TOTP key to the player's account.
     *
     * @param identifier the identifier of the name of the player to modify
     * @param totpKey the totp key to set
     * @return True upon success, false upon failure
     */
    boolean setTotpKey(NamedIdentifier identifier, String totpKey);

    /**
     * Removes the TOTP key if present of the given player's account.
     *
     * @param identifier the identifier of the name of the player to modify
     * @return True upon success, false upon failure
     */
    default boolean removeTotpKey(NamedIdentifier identifier) {
        return setTotpKey(identifier, null);
    }

    /**
     * Reload the data source.
     */
    @Override
    void reload();

    /**
     * Invalidate any cached data related to the specified player name.
     *
     * @param identifier the identifier of the player
     */
    default void invalidateCache(NamedIdentifier identifier) {
    }

    /**
     * Refresh any cached data (if present) related to the specified player name.
     *
     * @param identifiere the identifier of the player
     */
    default void refreshCache(NamedIdentifier identifiere) {
    }

}
