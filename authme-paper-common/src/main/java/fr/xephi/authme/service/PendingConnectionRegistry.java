package fr.xephi.authme.service;

import io.papermc.paper.connection.PlayerConnection;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Reserves a player name while its connection is still being established, before the play state.
 */
public class PendingConnectionRegistry {

    private final Map<String, Claim> claims = new ConcurrentHashMap<>();

    public PendingConnectionRegistry() {
    }

    public boolean tryClaim(String name, PlayerConnection connection, long ttlMillis) {
        String connectionKey = connectionKey(connection);
        long expiresAt = System.currentTimeMillis() + ttlMillis;
        Claim claim = claims.compute(normalize(name), (ignored, existing) -> {
            if (existing == null || existing.isStale() || existing.isHeldBy(connectionKey)) {
                return new Claim(connectionKey, connection, expiresAt);
            }
            return existing;
        });
        return claim.isHeldBy(connectionKey);
    }

    public boolean holdsClaim(String name, PlayerConnection connection) {
        Claim claim = claims.get(normalize(name));
        return claim != null && !claim.isStale() && claim.isHeldBy(connectionKey(connection));
    }

    public boolean hasLiveClaim(String name) {
        Claim claim = claims.get(normalize(name));
        return claim != null && !claim.isStale();
    }

    public void release(String name) {
        claims.remove(normalize(name));
    }

    // A refused duplicate connection closes too, so releasing by name alone would hand it the name
    public void releaseIfStale(String name) {
        claims.computeIfPresent(normalize(name), (ignored, claim) -> claim.isStale() ? null : claim);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    // The ephemeral port makes this unique per connection and unchanged across the phase switch
    private static String connectionKey(PlayerConnection connection) {
        return String.valueOf(connection.getAddress());
    }

    private static final class Claim {

        private final String connectionKey;
        private final PlayerConnection connection;
        private final long expiresAt;

        Claim(String connectionKey, PlayerConnection connection, long expiresAt) {
            this.connectionKey = connectionKey;
            this.connection = connection;
            this.expiresAt = expiresAt;
        }

        boolean isHeldBy(String otherConnectionKey) {
            return connectionKey.equals(otherConnectionKey);
        }

        // The TTL guarantees a claim can never lock a name out for good
        boolean isStale() {
            return !connection.isConnected() || System.currentTimeMillis() > expiresAt;
        }
    }
}
