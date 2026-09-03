package fr.xephi.authme.service;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores transient dialog state between Paper/Folia's configuration phase and the actual join.
 */
public class PreJoinDialogService {

    public record DialogSessionRecord<T>(UUID playerId, T internal) {
        /**
         * Verifies if it still matches the same player id we expect or if the client from this connection reconnected
         * with a different player id. If the player id does not match, an exception is thrown.
         *
         * @param playerId player id to check against the stored player id
         * @return the internal value if the player id matches
         * @throws IllegalArgumentException if the player id does not match
         */
        public T checkedReturn(UUID playerId) {
            if (!Objects.equals(this.playerId, playerId)) {
                throw new IllegalArgumentException("Player ID mismatch: expected " + this.playerId + ", got " + playerId);
            }

            return internal;
        }
    }

    private final Map<InetSocketAddress, DialogSessionRecord<String>> pendingLoginPasswords = new ConcurrentHashMap<>();
    private final Map<InetSocketAddress, DialogSessionRecord<String>> pendingRecoveryEmails = new ConcurrentHashMap<>();
    private final Map<InetSocketAddress, DialogSessionRecord<PendingRegistration>> pendingRegistrations = new ConcurrentHashMap<>();
    private final Set<UUID> skipPostJoinDialogs = ConcurrentHashMap.newKeySet();
    private final Map<UUID, String> pendingKickMessages = new ConcurrentHashMap<>();

    // Pre-join force-login: tracks players blocked in the pre-join login dialog so that
    // (DISABLED!!!) ForceLoginCommand can unblock them without requiring the player to be in PLAY state.

    // Multiple concurrent configure-phase sessions may exist for the same player name / UUID.
    // We assign an internal session id (long) to each registered future and map names -> deque of
    // session ids (registration order).
    private final AtomicLong nextSessionId = new AtomicLong(0);
    private final Map<String, Deque<Long>> pendingPreJoinByName = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<String>> pendingPreJoinFutures = new ConcurrentHashMap<>();
    private final Map<Long, UUID> sessionUuid = new ConcurrentHashMap<>();

    private final Map<InetSocketAddress, DialogSessionRecord<Boolean>> pendingForceLogins = new ConcurrentHashMap<>();

    public PreJoinDialogService() {
    }

    public void storePendingLoginPassword(InetSocketAddress conn, UUID playerId, String password) {
        pendingLoginPasswords.put(conn, new DialogSessionRecord<>(playerId, password));
    }

    public String consumePendingLoginPassword(InetSocketAddress conn, UUID playerId) {
        if (conn == null) {
            return null;
        }

        DialogSessionRecord<String> passwordRecord = pendingLoginPasswords.remove(conn);
        if (passwordRecord == null) return null;

        return passwordRecord.checkedReturn(playerId);
    }

    public void storePendingRecoveryEmail(InetSocketAddress conn, UUID playerId, String email) {
        pendingRecoveryEmails.put(conn, new DialogSessionRecord<>(playerId, email));
    }

    public String consumePendingRecoveryEmail(InetSocketAddress conn, UUID playerId) {
        DialogSessionRecord<String> emailRecord = pendingRecoveryEmails.remove(conn);
        if (emailRecord == null) return null;

        return emailRecord.checkedReturn(playerId);
    }

    public void storePendingPasswordRegistration(InetSocketAddress conn, UUID playerId, String password, String email) {
        pendingRegistrations.put(conn, new DialogSessionRecord<>(playerId, new PendingRegistration(password, email, false)));
    }

    public void storePendingEmailRegistration(InetSocketAddress conn, UUID playerId, String email) {
        PendingRegistration pendingRegistration = new PendingRegistration(email, null, true);
        pendingRegistrations.put(conn, new DialogSessionRecord<>(playerId, pendingRegistration));
    }

    public PendingRegistration consumePendingRegistration(InetSocketAddress conn, UUID playerId) {
        DialogSessionRecord<PendingRegistration> registrationRecord = pendingRegistrations.remove(conn);
        if (registrationRecord == null) return null;

        return registrationRecord.checkedReturn(playerId);
    }

    public void markSkipPostJoinDialog(UUID playerId) {
        skipPostJoinDialogs.add(playerId);
    }

    public boolean consumeSkipPostJoinDialog(InetSocketAddress address, UUID playerId) {
        return skipPostJoinDialogs.remove(playerId);
    }

    public void storePendingKickMessage(UUID playerId, String message) {
        pendingKickMessages.put(playerId, message);
    }

    public String consumePendingKickMessage(UUID playerId) {
        return pendingKickMessages.remove(playerId);
    }

    /**
     * Registers the blocking {@link CompletableFuture} used by the pre-join login dialog so that
     * {@link #approvePreJoinForceLogin} can resolve it from outside the event handler thread.
     *
     * @param normalizedName the player name in lowercase
     * @param uuid the player's UUID
     * @param future the future that blocks the configuration-phase thread
     */
    public long registerPreJoinFuture(String normalizedName, UUID uuid, CompletableFuture<String> future) {
        long sid = nextSessionId.incrementAndGet();

        pendingPreJoinFutures.put(sid, future);
        sessionUuid.put(sid, uuid);
        pendingPreJoinByName.computeIfAbsent(normalizedName, k -> new ConcurrentLinkedDeque<>()).addLast(sid);
        return sid;
    }

    /**
     * Unregister a previously registered pre-join future identified by its internal session id.
     */
    public void unregisterPreJoinFuture(long sessionId) {
        pendingPreJoinFutures.remove(sessionId);
        sessionUuid.remove(sessionId);
        for (Deque<Long> deque : pendingPreJoinByName.values()) {
            deque.remove(sessionId);
        }

        // clean up empty deques to avoid memory leaks
        pendingPreJoinByName.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Remove and return all session ids associated with the given player UUID.
     */
    private Set<Long> removeAllSessionsForUuid(UUID uuid) {
        Set<Long> removed = new HashSet<>();
        for (Map.Entry<Long, UUID> e : sessionUuid.entrySet()) {
            if (e.getValue().equals(uuid)) {
                long sid = e.getKey();
                removed.add(sid);
                pendingPreJoinFutures.remove(sid);
                sessionUuid.remove(sid);
                for (Deque<Long> deque : pendingPreJoinByName.values()) {
                    deque.remove(sid);
                }
            }
        }

        pendingPreJoinByName.entrySet().removeIf(e -> e.getValue().isEmpty());
        return removed;
    }

    /**
     * Approves a force-login for a player currently blocked in the pre-join login dialog.
     * Completes the blocking future with {@code null} (no kick message), allowing the player to
     * proceed to PLAY state where {@link #consumePendingForceLogin} will trigger a force-login.
     *
     * @param normalizedName the player name in lowercase
     * @return {@code true} if the player was in the pre-join dialog and the approval was registered,
     *         {@code false} if no such player was found (e.g. already joined or not in dialog)
     */
    public boolean approvePreJoinForceLogin(String normalizedName) {
        // the host needs to be verified so only a single session is approved that is associated with the proxy
        // throw new UnsupportedOperationException("Disable force logins from proxy and command for now with dialogs where multiple sessions may exist");

        return false;
    }

    /**
     * Approve a force-login for a specific session id. This completes only the future
     * associated with that session (if present) and marks the underlying player UUID
     * for force-login.
     *
     * @param sessionId the internal session id to approve
     * @return true if the session was found and approved
     */
    public boolean approvePreJoinForceLoginForSession(long sessionId) {
        // throw new UnsupportedOperationException("Disable force logins from proxy and command for now ");
        return false;

        /*
        CompletableFuture<String> future = pendingPreJoinFutures.remove(sessionId);
        UUID uuid = sessionUuid.remove(sessionId);
        // remove sessionId from any name queues
        for (Deque<Long> deque : pendingPreJoinByName.values()) {
            deque.remove(sessionId);
        }

        pendingPreJoinByName.entrySet().removeIf(e -> e.getValue().isEmpty());
        if (uuid == null) {
            return false;
        }

        pendingForceLogins.add(uuid);
        if (future != null) {
            future.complete(null);
        }

        return true;*/
    }

    /**
     * Consumes the force-login flag for the given player.
     *
     * @param playerId the player's UUID
     * @return {@code true} if a force-login was approved for this player (flag is cleared)
     */
    public boolean consumePendingForceLogin(UUID playerId) {
        DialogSessionRecord<Boolean> forceLogins = pendingForceLogins.remove(playerId);
        if (forceLogins == null) return false;

        return forceLogins.checkedReturn(playerId);
    }

    public void clear(UUID playerId) {
        //pendingLoginPasswords.remove(playerId);
        pendingRecoveryEmails.remove(playerId);
        pendingRegistrations.remove(playerId);
        skipPostJoinDialogs.remove(playerId);
        // Remove any pending force-login flags for this playerId
        pendingForceLogins.remove(playerId);
        pendingKickMessages.remove(playerId);
        // Remove all pre-join sessions associated with this playerId
        removeAllSessionsForUuid(playerId);
    }

    public record PendingRegistration(String primaryValue, String secondaryValue, boolean isEmailRegistration) {
    }
}
