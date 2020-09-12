package eu.authme.common.api;

import eu.authme.api.AuthEventListener;
import eu.authme.api.AuthMeAPI;
import eu.authme.api.User;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public abstract class AbstractAuthMeAPI implements AuthMeAPI {

  private Set<AuthEventListener> eventListeners;

  public AbstractAuthMeAPI() {
    this.eventListeners = new HashSet<>();
  }

  @Override
  public void registerEventListener(AuthEventListener eventListener) {
    eventListeners.add(eventListener);
  }

  public void callUserLogin(User user) {
    callEvent(listener -> listener.onUserLogin(user));
  }

  public void callUserChangePassword(User user) {
    callEvent(listener -> listener.onUserChangePassword(user));
  }

  public void callUserRegister(User user) {
    callEvent(listener -> listener.onUserRegister(user));
  }

  public void callUserUnregister(User user) {
    callEvent(listener -> listener.onUserUnregister(user));
  }

  private void callEvent(Consumer<AuthEventListener> listenerConsumer) {
    if (!eventListeners.isEmpty()) {
      for (AuthEventListener listener : eventListeners) {
        listenerConsumer.accept(listener);
      }
    }
  }
}
