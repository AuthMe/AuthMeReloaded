package eu.authme.api;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// todo: find a way to get this api for platforms with no service manager (singleton????)
/** Represents the base AuthMe API */
public interface AuthMeAPI {

  /**
   * Returns the user with the specified name. If the user has not been found in AuthMe's cache, it
   * will create a one.
   *
   * @param name name of the user you want to get
   * @return non-null user object
   */
  CompletableFuture<User> getUser(String name);

  /**
   * Returns the user with the specified name.
   *
   * @param name name of the user you want to get
   * @return user if present in AuthMe's cache, empty optional otherwise
   */
  Optional<User> getUserIfPresent(String name);

  /**
   * Returns the user with the specified unique id.
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
