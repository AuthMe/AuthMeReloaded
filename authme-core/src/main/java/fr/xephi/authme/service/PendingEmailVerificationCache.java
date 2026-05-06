package fr.xephi.authme.service;

import javax.inject.Inject;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds in-memory email confirmation requests that have not yet been verified by a code sent
 * to the new address. Entries expire after {@link #TTL_MS} (10 minutes).
 *
 * <p>A player is placed here when they run {@code /email add} or {@code /email change}.
 * Instead of saving the address immediately, a verification code is emailed to the new address.
 * The entry is consumed (and the address persisted) when the player submits the correct code
 * via {@code /email confirm <code>}. Expired entries are silently discarded.</p>
 */
public class PendingEmailVerificationCache {

    static final long TTL_MS = 10 * 60 * 1000L;

    private final ConcurrentHashMap<String, PendingEntry> pending = new ConcurrentHashMap<>();

    @Inject
    PendingEmailVerificationCache() {
    }

    /**
     * Registers a pending email confirmation for the given player. Any previous entry is replaced.
     * Also evicts expired entries.
     *
     * @param name  the player name (case-insensitive)
     * @param email the new email address awaiting confirmation
     * @param code  the verification code sent to that address
     */
    public void addPending(String name, String email, String code) {
        evictExpired();
        pending.put(name.toLowerCase(Locale.ROOT),
            new PendingEntry(email, code, System.currentTimeMillis() + TTL_MS));
    }

    /**
     * Returns whether a non-expired pending entry exists for the given player name.
     *
     * @param name the player name (case-insensitive)
     * @return true if a pending entry exists and has not yet expired
     */
    public boolean isPending(String name) {
        return getEntry(name) != null;
    }

    /**
     * Returns the pending entry for the given player name without removing it,
     * or {@code null} if no entry exists or it has expired.
     *
     * @param name the player name (case-insensitive)
     * @return the pending entry, or null
     */
    public PendingEntry getEntry(String name) {
        PendingEntry entry = pending.get(name.toLowerCase(Locale.ROOT));
        if (entry == null) {
            return null;
        }
        if (System.currentTimeMillis() > entry.expiresAt()) {
            pending.remove(name.toLowerCase(Locale.ROOT), entry);
            return null;
        }
        return entry;
    }

    /**
     * Removes and returns the pending entry for the given player name,
     * or {@code null} if no entry exists or it has expired.
     *
     * @param name the player name (case-insensitive)
     * @return the pending entry if valid, or null
     */
    public PendingEntry removePending(String name) {
        PendingEntry entry = pending.remove(name.toLowerCase(Locale.ROOT));
        if (entry == null || System.currentTimeMillis() > entry.expiresAt()) {
            return null;
        }
        return entry;
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(e -> now > e.getValue().expiresAt());
    }

    public record PendingEntry(String email, String code, long expiresAt) {}
}
