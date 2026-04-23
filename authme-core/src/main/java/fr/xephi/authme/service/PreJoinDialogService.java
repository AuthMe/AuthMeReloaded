package fr.xephi.authme.service;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores transient dialog state between Paper/Folia's configuration phase and the actual join.
 */
public class PreJoinDialogService {

    private final Map<UUID, String> pendingLoginPasswords = new ConcurrentHashMap<>();
    private final Map<UUID, PendingRegistration> pendingRegistrations = new ConcurrentHashMap<>();
    private final Set<UUID> skipPostJoinDialogs = ConcurrentHashMap.newKeySet();

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

    public void clear(UUID playerId) {
        pendingLoginPasswords.remove(playerId);
        pendingRegistrations.remove(playerId);
        skipPostJoinDialogs.remove(playerId);
    }

    public record PendingRegistration(String primaryValue, String secondaryValue, boolean isEmailRegistration) {
    }
}
