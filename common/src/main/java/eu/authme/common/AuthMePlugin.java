package eu.authme.common;

import eu.authme.api.AuthMeAPI;

public interface AuthMePlugin {

  void registerInServiceManager(AuthMeAPI api);
}
