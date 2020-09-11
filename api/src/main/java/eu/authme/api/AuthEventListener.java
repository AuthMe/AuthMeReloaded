package eu.authme.api;

/**
 * Represents auth event listener.
 */
public interface AuthEventListener {

  /**
   * Called when a {@link User} has logged in.
   *
   * @param user the user who logged in.
   */
  default void onUserLogin(User user) {}

  /**
   * Called when a {@link User} has changed password.
   *
   * @param user the user who changed password.
   */
  default void onUserChangePassword(User user) {}

  /**
   * Called when a {@link User} has registered.
   *
   * @param user the user who has registered.
   */
  default void onUserRegister(User user) {}

  /**
   * Called when a {@link User} has unregistered.
   *
   * @param user the user who has unregistered.
   */
  default void onUserUnregister(User user) {}
}
