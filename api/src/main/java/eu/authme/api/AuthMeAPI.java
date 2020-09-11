package eu.authme.api;

import java.util.Optional;
import java.util.UUID;

// todo: find a way to get this api for platforms with no service manager (singleton????)
/** Represents the base AuthMe API */
public interface AuthMeAPI {

  /**
   * Returns the user with the specified name. If the user has not been found in AuthMe's cache, it
   * will create a one.
   *
   * <p><b>WARNING: This may or may not be a thread blocking method. Call
   * asynchronously!!!!!!1!!11!!!!</b>
   *
   * @param name name of the user you want to get
   * @return non-null user object
   */
  User getUser(String name);

  /**
   * Returns the user with the specified name.
   *
   * <p><b>WARNING: This is a thread blocking method. Call asynchronously!!!!!!1!!11!!!!</b>
   *
   * @param name name of the user you want to get
   * @return user if present in AuthMe's cache, empty optional otherwise
   */
  Optional<User> getUserIfPresent(String name);

  /**
   * Returns the user with the specified unique id.
   *
   * <p><b>WARNING: This is a thread blocking method. Call asynchronously!!!!!!1!!11!!!!</b>
   *
   * @param uuid uuid of the user you want to get
   * @return user if present in AuthMe's cache, empty optional otherwise
   */
  Optional<User> getUserIfPresent(UUID uuid);

  /**
   * Registers the specified {@link AuthEventListener}
   *
   * @param eventListener event listener
   */
  void registerEventListener(AuthEventListener eventListener);
}
