package fr.xephi.authme.service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores transient dialog state between Paper/Folia's configuration phase and the actual join.
 * <p>
 * State belongs to a session opened for one configuration-phase connection, never to a player name
 * or profile id: those are shared by every connection authenticating as the same player on an
 * offline-mode server, so a later connection could otherwise consume or wipe the state of the one
 * that is still authenticating.
 */
public class PreJoinDialogService {

    private final AtomicLong nextSessionId = new AtomicLong();
    private final Map<String, Long> sessionByName = new ConcurrentHashMap<>();
    private final Map<Long, DialogSession> sessions = new ConcurrentHashMap<>();

    public PreJoinDialogService() {
    }

    /**
     * Opens a session for a connection entering the configuration phase, dropping the state of any
     * earlier session for that name so it can never be inherited.
     *
     * @param normalizedName the player name in lowercase
     * @return the id identifying this session in every other call
     */
    public long openSession(String normalizedName) {
        long sessionId = nextSessionId.incrementAndGet();
        sessions.put(sessionId, new DialogSession(normalizedName));

        Long previousId = sessionByName.put(normalizedName, sessionId);
        if (previousId != null) {
            sessions.remove(previousId);
        }
        return sessionId;
    }

    public void retireSession(long sessionId) {
        DialogSession session = sessions.remove(sessionId);
        if (session != null) {
            sessionByName.remove(session.name, sessionId);
        }
    }

    /**
     * Consumes everything the player's session holds, at once so that nothing can be left half-read.
     *
     * @param normalizedName the player name in lowercase
     * @return the pending state, or {@link PendingDialogState#NONE} if the player has no session
     */
    public PendingDialogState consumeSession(String normalizedName) {
        Long sessionId = sessionByName.remove(normalizedName);
        if (sessionId == null) {
            return PendingDialogState.NONE;
        }

        DialogSession session = sessions.remove(sessionId);
        return session == null ? PendingDialogState.NONE : session.toPendingState();
    }

    public void storePendingLoginPassword(long sessionId, String password) {
        DialogSession session = sessions.get(sessionId);
        if (session != null) {
            session.loginPassword = password;
        }
    }

    public void storePendingRecoveryEmail(long sessionId, String email) {
        DialogSession session = sessions.get(sessionId);
        if (session != null) {
            session.recoveryEmail = email;
        }
    }

    public void storePendingPasswordRegistration(long sessionId, String password, String email) {
        DialogSession session = sessions.get(sessionId);
        if (session != null) {
            session.registration = new PendingRegistration(password, email, false);
        }
    }

    public void storePendingEmailRegistration(long sessionId, String email) {
        DialogSession session = sessions.get(sessionId);
        if (session != null) {
            session.registration = new PendingRegistration(email, null, true);
        }
    }

    public void markSkipPostJoinDialog(long sessionId) {
        DialogSession session = sessions.get(sessionId);
        if (session != null) {
            session.skipPostJoinDialog = true;
        }
    }

    public void storePendingKickMessage(long sessionId, String message) {
        DialogSession session = sessions.get(sessionId);
        if (session != null) {
            session.kickMessage = message;
        }
    }

    /**
     * Registers the blocking {@link CompletableFuture} used by the pre-join dialog so that
     * {@link #approvePreJoinForceLogin} can resolve it from outside the event handler thread.
     *
     * @param sessionId the session waiting on the dialog
     * @param future the future that blocks the configuration-phase thread
     */
    public void registerPreJoinFuture(long sessionId, CompletableFuture<String> future) {
        DialogSession session = sessions.get(sessionId);
        if (session != null) {
            session.future = future;
        }
    }

    public void unregisterPreJoinFuture(long sessionId) {
        DialogSession session = sessions.get(sessionId);
        if (session != null) {
            session.future = null;
        }
    }

    /**
     * Approves a force-login for a player currently blocked in the pre-join dialog, letting him
     * proceed to the play state where the force-login is performed. Resolved by name because a proxy
     * or the console only knows the name; unambiguous because a name has at most one session.
     *
     * @param normalizedName the player name in lowercase
     * @return {@code true} if the player was in the pre-join dialog and the approval was registered
     */
    public boolean approvePreJoinForceLogin(String normalizedName) {
        Long sessionId = sessionByName.get(normalizedName);
        DialogSession session = sessionId == null ? null : sessions.get(sessionId);
        CompletableFuture<String> future = session == null ? null : session.future;
        if (future == null) {
            return false;
        }

        session.forceLogin = true;
        future.complete(null);
        return true;
    }

    private static final class DialogSession {

        private final String name;

        // Written on the configuration-phase thread, read on the join thread
        private volatile String loginPassword;
        private volatile String recoveryEmail;
        private volatile PendingRegistration registration;
        private volatile boolean skipPostJoinDialog;
        private volatile boolean forceLogin;
        private volatile String kickMessage;
        private volatile CompletableFuture<String> future;

        DialogSession(String name) {
            this.name = name;
        }

        PendingDialogState toPendingState() {
            return new PendingDialogState(loginPassword, recoveryEmail, registration,
                skipPostJoinDialog, forceLogin, kickMessage);
        }
    }

    public record PendingDialogState(String loginPassword, String recoveryEmail,
                                     PendingRegistration registration, boolean skipPostJoinDialog,
                                     boolean forceLogin, String kickMessage) {

        public static final PendingDialogState NONE =
            new PendingDialogState(null, null, null, false, false, null);
    }

    public record PendingRegistration(String primaryValue, String secondaryValue, boolean isEmailRegistration) {
    }
}
