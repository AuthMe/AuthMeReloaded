package fr.xephi.authme.service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds in-memory premium verification requests that have not yet been confirmed by a
 * cryptographic Mojang session check. Entries expire after {@link #TTL_MS} (5 minutes).
 *
 * <p>A player is placed here when they run {@code /premium} (or an admin runs
 * {@code /authme premium}) on an offline-mode backend. The entry is consumed (and the
 * UUID persisted to the database) when the player reconnects and the Mojang session check
 * succeeds. If the check fails, the entry is removed without saving.</p>
 */
public class PendingPremiumCache {

    static final long TTL_MS = 5 * 60 * 1000L;

    private final ConcurrentHashMap<String, PendingEntry> pending = new ConcurrentHashMap<>();

    @Inject
    PendingPremiumCache() {
    }

    /**
     * Registers a pending premium verification for the given player name.
     * Also evicts any already-expired entries.
     *
     * @param name      the player name (case-insensitive)
     * @param mojangUuid the Mojang UUID fetched from the profile API
     * @return the names of any entries that were evicted due to expiry
     */
    public Collection<String> addPending(String name, UUID mojangUuid) {
        Collection<String> evicted = evictExpired();
        pending.put(name.toLowerCase(Locale.ROOT),
            new PendingEntry(mojangUuid, System.currentTimeMillis() + TTL_MS));
        return evicted;
    }

    /**
     * Returns whether a non-expired pending entry exists for the given player name.
     *
     * @param name the player name (case-insensitive)
     * @return true if a pending entry exists and has not yet expired
     */
    public boolean isPending(String name) {
        return getPendingUuid(name) != null;
    }

    /**
     * Returns the Mojang UUID associated with the pending entry for the given player name,
     * or {@code null} if no entry exists or it has expired.
     *
     * @param name the player name (case-insensitive)
     * @return the pending Mojang UUID, or null
     */
    public UUID getPendingUuid(String name) {
        PendingEntry entry = pending.get(name.toLowerCase(Locale.ROOT));
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAt()) {
            pending.remove(name.toLowerCase(Locale.ROOT));
            return null;
        }
        return entry.mojangUuid();
    }

    /**
     * Removes and returns the pending Mojang UUID for the given player name.
     *
     * @param name the player name (case-insensitive)
     * @return the pending Mojang UUID if a valid entry existed, or null
     */
    public UUID removePending(String name) {
        PendingEntry entry = pending.remove(name.toLowerCase(Locale.ROOT));
        if (entry == null || System.currentTimeMillis() > entry.expiresAt()) {
            return null;
        }
        return entry.mojangUuid();
    }

    /**
     * Removes all expired entries and returns their player names so the caller can
     * notify the proxy (via {@code PREMIUM_UNSET}) that the pending state is gone.
     *
     * @return the names of evicted entries (lowercase)
     */
    public Collection<String> evictExpired() {
        long now = System.currentTimeMillis();
        List<String> evicted = new ArrayList<>();
        pending.entrySet().removeIf(e -> {
            if (now > e.getValue().expiresAt()) {
                evicted.add(e.getKey());
                return true;
            }
            return false;
        });
        return evicted;
    }

    private record PendingEntry(UUID mojangUuid, long expiresAt) {}
}
