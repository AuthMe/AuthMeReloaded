package fr.xephi.authme.api.v3;

import java.time.Instant;
import java.util.UUID;

/**
 * Read-only player info exposed in the AuthMe API.
 *
 * @see AuthMeApi#getPlayerInfo
 */
public interface AuthMePlayer {

    /**
     * @return the case-sensitive name of the player, e.g. "thePlayer3030" - never null
     */
    String getName();

    /**
     * Returns the UUID of the player as given by the server (may be offline UUID or not).
     * The UUID is null if AuthMe is configured not to store the UUID or if the data is not
     * present (e.g. older record).
     *
     * @return player uuid, or null if not available
     */
    UUID getUuid();

    /**
     * Returns the email address associated with this player, or null if not available.
     *
     * @return player's email or null
     */
    String getEmail();

    /**
     * @return the registration date of the player's account - never null
     */
    Instant getRegistrationDate();

    /**
     * Returns the IP address with which the player's account was registered. May be null
     * for older accounts, or if the account was registered by someone else (e.g. by an admin).
     *
     * @return the ip address used during the registration of the account, or null
     */
    String getRegistrationIpAddress();

    /**
     * Returns the last login date of the player. May be null if the player never logged in.
     *
     * @return date the player last logged in successfully, or null if not applicable
     */
    Instant getLastLoginDate();

    /**
     * Returns the IP address the player last logged in with successfully. May be null if the
     * player never logged in.
     *
     * @return ip address the player last logged in with successfully, or null if not applicable
     */
    String getLastLoginIpAddress();

}
