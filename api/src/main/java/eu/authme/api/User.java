package eu.authme.api;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents an (un)authenticated user.
 */
public interface User {

  /**
   * Returns the name of the user.
   *
   * @return name
   */
  String getName();

  /**
   * Returns the unique id of the user.
   *
   * @return unique id
   */
  UUID getUniqueId();

  /**
   * Returns whether or not this user has logged in.
   *
   * @return boolean value
   */
  boolean hasLoggedIn();

  /**
   * Returns whether or not this user is registered.
   *
   * @return boolean value
   */
  boolean isRegistered();

  /**
   * Force logins this user if he isn't logged in yet.
   */
  void forceLogin();

  /**
   * Force registers this user with the specified password if he isn't registered yet.
   *
   * @param password the password you want to register the user with.
   */
  void forceRegister(String password);

  /**
   * Returns the date on which this user has registered.
   *
   * @return registration date if present, empty optional otherwise
   */
  Optional<Instant> getRegistrationDate();

  /**
   * Returns the date on which this user has last logged in.
   *
   * @return last login date if present, empty optional otherwise
   */
  Optional<Instant> getLastLoginDate();

  /**
   * Returns the email of the user
   *
   * @return email if present, empty optional otherwise
   */
  Optional<String> getEmail();
}
