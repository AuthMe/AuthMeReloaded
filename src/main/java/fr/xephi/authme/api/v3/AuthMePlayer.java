package fr.xephi.authme.api.v3;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Read-only player info exposed in the AuthMe API. The data in this object is copied from the
 * database and not updated afterwards. As such, it may become outdated if the player data changes
 * in AuthMe.
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
     * The UUID is not present if AuthMe is configured not to store the UUID or if the data is not
     * present (e.g. older record).
     *
     * @return player uuid, or empty optional if not available
     */
    Optional<UUID> getUuid();

    /**
     * Returns the email address associated with this player, or an empty optional if not available.
     *
     * @return player's email or empty optional
     */
    Optional<String> getEmail();

    /**
     * @return the registration date of the player's account - never null
     */
    Instant getRegistrationDate();

    /**
     * Returns the IP address with which the player's account was registered. Returns an empty optional
     * for older accounts, or if the account was registered by someone else (e.g. by an admin).
     *
     * @return the ip address used during the registration of the account, or empty optional
     */
    Optional<String> getRegistrationIpAddress();

    /**
     * Returns the last login date of the player. An empty optional is returned if the player never logged in.
     *
     * @return date the player last logged in successfully, or empty optional if not applicable
     */
    Optional<Instant> getLastLoginDate();

    /**
     * Returns the IP address the player last logged in with successfully. Returns an empty optional if the
     * player never logged in.
     *
     * @return ip address the player last logged in with successfully, or empty optional if not applicable
     */
    Optional<String> getLastLoginIpAddress();

}
