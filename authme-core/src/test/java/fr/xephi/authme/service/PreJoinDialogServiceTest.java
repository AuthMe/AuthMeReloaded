package fr.xephi.authme.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link PreJoinDialogService}.
 */
class PreJoinDialogServiceTest {

    @Test
    void shouldStoreAndConsumePendingLoginPassword() {
        PreJoinDialogService service = new PreJoinDialogService();
        UUID uuid = UUID.randomUUID();

        service.storePendingLoginPassword(uuid, "s3cr3t");

        assertThat(service.consumePendingLoginPassword(uuid), is("s3cr3t"));
        assertThat(service.consumePendingLoginPassword(uuid), nullValue());
    }

    @Test
    void shouldApprovePreJoinForceLoginAndCompleteFuture() {
        PreJoinDialogService service = new PreJoinDialogService();
        UUID uuid = UUID.randomUUID();
        CompletableFuture<String> future = new CompletableFuture<>();
        service.registerPreJoinFuture("bobby", uuid, future);

        boolean result = service.approvePreJoinForceLogin("bobby");

        assertThat(result, is(true));
        assertThat(future.isDone(), is(true));
        assertThat(future.getNow("sentinel"), is(nullValue()));
        assertThat(service.consumePendingForceLogin(uuid), is(true));
        assertThat(service.consumePendingForceLogin(uuid), is(false));
    }

    @Test
    void shouldReturnFalseForApproveWhenNoPreJoinDialogPending() {
        PreJoinDialogService service = new PreJoinDialogService();

        assertThat(service.approvePreJoinForceLogin("nobody"), is(false));
    }

    @Test
    void shouldReturnFalseForConsumeForceLoginWhenNotApproved() {
        PreJoinDialogService service = new PreJoinDialogService();

        assertThat(service.consumePendingForceLogin(UUID.randomUUID()), is(false));
    }

    @Test
    void shouldNotApproveAfterUnregister() {
        PreJoinDialogService service = new PreJoinDialogService();
        UUID uuid = UUID.randomUUID();
        CompletableFuture<String> future = new CompletableFuture<>();
        service.registerPreJoinFuture("alice", uuid, future);
        service.unregisterPreJoinFuture(uuid);

        boolean result = service.approvePreJoinForceLogin("alice");

        assertThat(result, is(false));
        assertThat(future.isDone(), is(false));
    }

    @Test
    void shouldClearAllStateForPlayer() {
        PreJoinDialogService service = new PreJoinDialogService();
        UUID uuid = UUID.randomUUID();
        CompletableFuture<String> future = new CompletableFuture<>();
        service.storePendingLoginPassword(uuid, "pw");
        service.registerPreJoinFuture("charlie", uuid, future);

        service.clear(uuid);

        assertThat(service.consumePendingLoginPassword(uuid), is(nullValue()));
        assertThat(service.approvePreJoinForceLogin("charlie"), is(false));
        assertThat(service.consumePendingForceLogin(uuid), is(false));
    }
}
