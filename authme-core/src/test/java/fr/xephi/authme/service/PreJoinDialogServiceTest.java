package fr.xephi.authme.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link PreJoinDialogService}.
 */
class PreJoinDialogServiceTest {

    private PreJoinDialogService service;

    @BeforeEach
    void createService() {
        service = new PreJoinDialogService();
    }

    @Test
    void shouldStoreAndConsumePendingLoginPassword() {
        long sessionId = service.openSession("bobby");

        service.storePendingLoginPassword(sessionId, "s3cr3t");

        assertThat(service.consumeSession("bobby").loginPassword(), is("s3cr3t"));
        assertThat(service.consumeSession("bobby").loginPassword(), nullValue());
    }

    @Test
    void shouldConsumeWholeSessionAtOnce() {
        long sessionId = service.openSession("bobby");
        service.storePendingLoginPassword(sessionId, "s3cr3t");
        service.storePendingRecoveryEmail(sessionId, "bob@example.com");
        service.storePendingPasswordRegistration(sessionId, "pw", "reg@example.com");
        service.markSkipPostJoinDialog(sessionId);
        service.storePendingKickMessage(sessionId, "bye");

        PreJoinDialogService.PendingDialogState state = service.consumeSession("bobby");

        assertThat(state.loginPassword(), is("s3cr3t"));
        assertThat(state.recoveryEmail(), is("bob@example.com"));
        assertThat(state.registration(), is(new PreJoinDialogService.PendingRegistration("pw", "reg@example.com", false)));
        assertThat(state.skipPostJoinDialog(), is(true));
        assertThat(state.kickMessage(), is("bye"));
    }

    @Test
    void shouldReturnEmptyStateForPlayerWithoutSession() {
        PreJoinDialogService.PendingDialogState state = service.consumeSession("nobody");

        assertThat(state, is(PreJoinDialogService.PendingDialogState.NONE));
        assertThat(state.loginPassword(), nullValue());
        assertThat(state.forceLogin(), is(false));
    }

    @Test
    void shouldStoreEmailRegistration() {
        long sessionId = service.openSession("bobby");

        service.storePendingEmailRegistration(sessionId, "bob@example.com");

        assertThat(service.consumeSession("bobby").registration(),
            is(new PreJoinDialogService.PendingRegistration("bob@example.com", null, true)));
    }

    @Test
    void shouldNotLetLaterSessionConsumeStateOfEarlierOne() {
        long firstSession = service.openSession("bobby");
        service.storePendingLoginPassword(firstSession, "s3cr3t");

        service.openSession("bobby");

        assertThat(service.consumeSession("bobby").loginPassword(), nullValue());
        // The earlier session is gone, so writing to it must not resurrect it either
        service.storePendingLoginPassword(firstSession, "s3cr3t");
        assertThat(service.consumeSession("bobby").loginPassword(), nullValue());
    }

    @Test
    void shouldIgnoreStoresForRetiredSession() {
        long sessionId = service.openSession("bobby");
        service.retireSession(sessionId);

        service.storePendingLoginPassword(sessionId, "s3cr3t");
        service.markSkipPostJoinDialog(sessionId);

        assertThat(service.consumeSession("bobby"), is(PreJoinDialogService.PendingDialogState.NONE));
    }

    @Test
    void shouldApprovePreJoinForceLoginAndCompleteFuture() {
        long sessionId = service.openSession("bobby");
        CompletableFuture<String> future = new CompletableFuture<>();
        service.registerPreJoinFuture(sessionId, future);

        boolean result = service.approvePreJoinForceLogin("bobby");

        assertThat(result, is(true));
        assertThat(future.isDone(), is(true));
        assertThat(future.getNow("sentinel"), is(nullValue()));
        assertThat(service.consumeSession("bobby").forceLogin(), is(true));
        assertThat(service.consumeSession("bobby").forceLogin(), is(false));
    }

    @Test
    void shouldReturnFalseForApproveWhenNoPreJoinDialogPending() {
        assertThat(service.approvePreJoinForceLogin("nobody"), is(false));
    }

    @Test
    void shouldReturnFalseForApproveWhenSessionHasNoDialog() {
        service.openSession("bobby");

        assertThat(service.approvePreJoinForceLogin("bobby"), is(false));
    }

    @Test
    void shouldNotApproveAfterUnregister() {
        long sessionId = service.openSession("alice");
        CompletableFuture<String> future = new CompletableFuture<>();
        service.registerPreJoinFuture(sessionId, future);
        service.unregisterPreJoinFuture(sessionId);

        boolean result = service.approvePreJoinForceLogin("alice");

        assertThat(result, is(false));
        assertThat(future.isDone(), is(false));
    }

    @Test
    void shouldNotApproveAfterSessionRetired() {
        long sessionId = service.openSession("charlie");
        CompletableFuture<String> future = new CompletableFuture<>();
        service.storePendingLoginPassword(sessionId, "pw");
        service.registerPreJoinFuture(sessionId, future);

        service.retireSession(sessionId);

        assertThat(service.approvePreJoinForceLogin("charlie"), is(false));
        assertThat(service.consumeSession("charlie"), is(PreJoinDialogService.PendingDialogState.NONE));
    }
}
