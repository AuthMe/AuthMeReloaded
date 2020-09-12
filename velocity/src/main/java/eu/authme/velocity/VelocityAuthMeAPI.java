package eu.authme.velocity;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import eu.authme.api.User;
import eu.authme.common.api.AbstractAuthMeAPI;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityAuthMeAPI extends AbstractAuthMeAPI {

  private final AuthMeVelocity plugin;
  private Map<UUID, VelocityUser> userCache = new ConcurrentHashMap<>();
  private Map<String, CompletableFuture<User>> pendingFutures = new ConcurrentHashMap<>();

  public VelocityAuthMeAPI(AuthMeVelocity plugin) {
    this.plugin = plugin;
  }

  @Override
  public CompletableFuture<User> getUser(String name) {
    for (VelocityUser user : userCache.values()) {
      if (user.getName().equalsIgnoreCase(name)) {
        return CompletableFuture.completedFuture(user);
      }
    }
    CompletableFuture<User> future = new CompletableFuture<>();
    ByteArrayDataOutput out = ByteStreams.newDataOutput();
    out.writeUTF("GetUser");
    out.writeUTF(name);
    // todo
    try {
      return future;
    } finally {
      pendingFutures.put(name, future);
    }
  }

  @Override
  public Optional<User> getUserIfPresent(String name) {
    for (VelocityUser user : userCache.values()) {
      if (user.getName().equalsIgnoreCase(name)) {
        return Optional.of(user);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<User> getUserIfPresent(UUID uuid) {
    return Optional.ofNullable(userCache.get(uuid));
  }

  public void putToUserCache(VelocityUser user) {
    userCache.put(user.getUniqueId(), user);
  }

  public void modify(VelocityUser user) {}
}
