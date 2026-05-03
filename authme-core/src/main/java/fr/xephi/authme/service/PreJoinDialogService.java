package fr.xephi.authme.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores transient dialog state between Paper/Folia's configuration phase and the actual join.
 */
public class PreJoinDialogService {

    private final Map<UUID, String> pendingLoginPasswords = new ConcurrentHashMap<>();
    private final Map<UUID, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();
    private final Set<UUID> skipPostJoinDialogs = ConcurrentHashMap.newKeySet();

    // Pre-join force-login: tracks players blocked in the pre-join login dialog so that
    // ForceLoginCommand can unblock them without requiring the player to be in PLAY state.
    private final Map<String, UUID> pendingPreJoinByName = new ConcurrentHashMap<>();
    private final Map<UUID, CompletableFuture<String>> pendingPreJoinFutures = new ConcurrentHashMap<>();
    private final Set<UUID> pendingForceLogins = ConcurrentHashMap.newKeySet();

    public PreJoinDialogService() {
    }

    public void storePendingLoginPassword(UUID playerId, String password) {
        pendingLoginPasswords.put(playerId, password);
    }

    public String consumePendingLoginPassword(UUID playerId) {
        return pendingLoginPasswords.remove(playerId);
    }

    public void storePendingPasswordRegistration(UUID playerId, String password, String email) {
        pendingRegistrations.put(playerId, new PendingRegistration(password, email, false));
    }

    public void storePendingEmailRegistration(UUID playerId, String email) {
        pendingRegistrations.put(playerId, new PendingRegistration(email, null, true));
    }

    public PendingRegistration consumePendingRegistration(UUID playerId) {
        return pendingRegistrations.remove(playerId);
    }

    public void markSkipPostJoinDialog(UUID playerId) {
        skipPostJoinDialogs.add(playerId);
    }

    public boolean consumeSkipPostJoinDialog(UUID playerId) {
        return skipPostJoinDialogs.remove(playerId);
    }

    /**
     * Registers the blocking {@link CompletableFuture} used by the pre-join login dialog so that
     * {@link #approvePreJoinForceLogin} can resolve it from outside the event handler thread.
     *
     * @param normalizedName the player name in lowercase
     * @param uuid the player's UUID
     * @param future the future that blocks the configuration-phase thread
     */
    public void registerPreJoinFuture(String normalizedName, UUID uuid, CompletableFuture<String> future) {
        pendingPreJoinByName.put(normalizedName, uuid);
        pendingPreJoinFutures.put(uuid, future);
    }

    /**
     * Removes the pre-join future registration once the blocking wait is over.
     *
     * @param uuid the player's UUID
     */
    public void unregisterPreJoinFuture(UUID uuid) {
        pendingPreJoinFutures.remove(uuid);
        pendingPreJoinByName.values().remove(uuid);
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
        UUID uuid = pendingPreJoinByName.get(normalizedName);
        if (uuid == null) {
            return false;
        }
        CompletableFuture<String> future = pendingPreJoinFutures.get(uuid);
        if (future == null) {
            return false;
        }
        pendingForceLogins.add(uuid);
        future.complete(null);
        return true;
    }

    /**
     * Consumes the force-login flag for the given player.
     *
     * @param playerId the player's UUID
     * @return {@code true} if a force-login was approved for this player (flag is cleared)
     */
    public boolean consumePendingForceLogin(UUID playerId) {
        return pendingForceLogins.remove(playerId);
    }

    public void clear(UUID playerId) {
        pendingLoginPasswords.remove(playerId);
        pendingRegistrations.remove(playerId);
        skipPostJoinDialogs.remove(playerId);
        pendingForceLogins.remove(playerId);
        unregisterPreJoinFuture(playerId);
    }

    public record PendingRegistration(String primaryValue, String secondaryValue, boolean isEmailRegistration) {
    }
}
