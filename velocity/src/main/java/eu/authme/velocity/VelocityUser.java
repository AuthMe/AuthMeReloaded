package eu.authme.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import eu.authme.api.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public class VelocityUser implements User {

  private final VelocityAuthMeAPI api;
  private final ProxyServer proxy;
  private final String name;
  private String email;
  private final UUID uuid;
  private boolean loggedIn, registered;
  private Instant registrationDate;
  private Instant lastLoginDate;

  public VelocityUser(
      VelocityAuthMeAPI api,
      ProxyServer proxy,
      String name,
      String email,
      UUID uuid,
      boolean loggedIn,
      boolean registered,
      Instant registrationDate,
      Instant lastLoginDate) {
    this.name = name;
    this.email = email;
    this.uuid = uuid;
    this.loggedIn = loggedIn;
    this.registered = registered;
    this.registrationDate = registrationDate;
    this.lastLoginDate = lastLoginDate;
    this.api = api;
    this.proxy = proxy;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public UUID getUniqueId() {
    return uuid;
  }

  @Override
  public boolean hasLoggedIn() {
    return loggedIn;
  }

  public void setLoggedIn(boolean loggedIn) {
    this.loggedIn = loggedIn;
    if (loggedIn) {
      api.callUserLogin(this);
    }
  }

  @Override
  public boolean isRegistered() {
    return registered;
  }

  public void setRegistered(boolean registered) {
    this.registered = registered;
    if (registered) {
      api.callUserRegister(this);
    } else {
      api.callUserUnregister(this);
    }
  }

  @Override
  public void forceLogin() {
    Optional<Player> playerOpt = proxy.getPlayer(name);
    if (playerOpt.isPresent()) {
      Player player = playerOpt.get();
      if (!loggedIn && player.getCurrentServer().isPresent()) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ForceLogin");
        out.writeUTF(player.getUniqueId().toString());
        // todo: make sure is in auth server
        player.getCurrentServer().get().sendPluginMessage(AuthMeVelocity.MESSAGING, out.toByteArray());
      }
    }
  }

  @Override
  public void forceRegister(String password) {
    Optional<Player> playerOpt = proxy.getPlayer(name);
    if (playerOpt.isPresent()) {
      Player player = playerOpt.get();
      if (!registered && player.getCurrentServer().isPresent()) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("ForceRegister");
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(password);
        // todo: make sure is in auth server
        player.getCurrentServer().get().sendPluginMessage(AuthMeVelocity.MESSAGING, out.toByteArray());
      }
    }
  }

  @Override
  public Optional<Instant> getRegistrationDate() {
    return Optional.ofNullable(registrationDate);
  }

  public void setRegistrationDate(Instant date) {
    this.registrationDate = date;
  }

  @Override
  public Optional<Instant> getLastLoginDate() {
    return Optional.ofNullable(lastLoginDate);
  }

  public void setLastLoginDate(Instant date) {
    this.lastLoginDate = date;
  }

  @Override
  public Optional<String> getEmail() {
    return Optional.ofNullable(email);
  }

  public void setEmail(String email) {
    this.email = email;
  }
}
