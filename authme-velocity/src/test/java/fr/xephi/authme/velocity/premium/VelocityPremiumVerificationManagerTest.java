package fr.xephi.authme.velocity.premium;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class VelocityPremiumVerificationManagerTest {

    @Test
    void shouldForceOnlineModeForPremiumUser() {
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), "alice"::equals, normalizedName -> false, name -> { }, () -> false);

        PreLoginEvent event = new PreLoginEvent(mock(InboundConnection.class), "Alice", null);
        manager.onPreLogin(event);

        assertEquals(PreLoginEvent.PreLoginComponentResult.forceOnlineMode().toString(),
            event.getResult().toString());
    }

    @Test
    void shouldRewriteVerifiedProfileToOfflineUuid() {
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), "alice"::equals, normalizedName -> false, name -> { }, () -> true);
        UUID mojangUuid = UUID.fromString("8d6d0684-d8b4-4d40-8d2d-0dd4df5555c8");
        GameProfile originalProfile = new GameProfile(mojangUuid, "Alice", List.of());
        GameProfileRequestEvent event = new GameProfileRequestEvent(
            mock(InboundConnection.class), originalProfile, true);

        manager.onGameProfileRequest(event);

        assertEquals(UuidUtils.generateOfflinePlayerUuid("Alice"), event.getGameProfile().getId());
        assertEquals(mojangUuid, manager.getVerifiedPremiumUuid("alice"));
    }

    @Test
    void shouldIgnoreOfflineModeProfileRequest() {
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), "alice"::equals, normalizedName -> false, name -> { }, () -> true);
        UUID mojangUuid = UUID.fromString("8d6d0684-d8b4-4d40-8d2d-0dd4df5555c8");
        GameProfile originalProfile = new GameProfile(mojangUuid, "Alice", List.of());
        GameProfileRequestEvent event = new GameProfileRequestEvent(
            mock(InboundConnection.class), originalProfile, false);

        manager.onGameProfileRequest(event);

        assertEquals(mojangUuid, event.getGameProfile().getId());
        assertNull(manager.getVerifiedPremiumUuid("alice"));
    }

    @Test
    void shouldKeepMojangUuidWhenOfflineCompatibilityDisabled() {
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), "alice"::equals, normalizedName -> false, name -> { }, () -> false);
        UUID mojangUuid = UUID.fromString("8d6d0684-d8b4-4d40-8d2d-0dd4df5555c8");
        GameProfile originalProfile = new GameProfile(mojangUuid, "Alice", List.of());
        GameProfileRequestEvent event = new GameProfileRequestEvent(
            mock(InboundConnection.class), originalProfile, true);

        manager.onGameProfileRequest(event);

        assertEquals(mojangUuid, event.getGameProfile().getId());
        assertEquals(mojangUuid, manager.getVerifiedPremiumUuid("alice"));
    }

    @Test
    void shouldForceOnlineModeOnFirstPendingAttemptThenCancelOnSecond() {
        Set<String> pending = new HashSet<>(Set.of("alice"));
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), pending::contains, pending::contains, pending::remove, () -> false);

        PreLoginEvent firstAttempt = new PreLoginEvent(mock(InboundConnection.class), "Alice", null);
        manager.onPreLogin(firstAttempt);
        // Mojang rejected the session: no profile request, no login and no disconnect event follow
        PreLoginEvent secondAttempt = new PreLoginEvent(mock(InboundConnection.class), "Alice", null);
        manager.onPreLogin(secondAttempt);

        assertEquals(PreLoginEvent.PreLoginComponentResult.forceOnlineMode().toString(),
            firstAttempt.getResult().toString());
        assertEquals(PreLoginEvent.PreLoginComponentResult.allowed().toString(),
            secondAttempt.getResult().toString());
        assertFalse(pending.contains("alice"));
    }

    @Test
    void shouldKeepForcingOnlineModeForPendingPlayerAfterSuccessfulVerification() {
        Set<String> pending = new HashSet<>(Set.of("alice"));
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), pending::contains, pending::contains, pending::remove, () -> false);
        GameProfile profile = new GameProfile(
            UUID.fromString("8d6d0684-d8b4-4d40-8d2d-0dd4df5555c8"), "Alice", List.of());

        manager.onPreLogin(new PreLoginEvent(mock(InboundConnection.class), "Alice", null));
        manager.onGameProfileRequest(new GameProfileRequestEvent(mock(InboundConnection.class), profile, true));
        // The backend has not finalized the enrollment yet, so the player is still pending on reconnect
        PreLoginEvent reconnect = new PreLoginEvent(mock(InboundConnection.class), "Alice", null);
        manager.onPreLogin(reconnect);

        assertEquals(PreLoginEvent.PreLoginComponentResult.forceOnlineMode().toString(),
            reconnect.getResult().toString());
        assertTrue(pending.contains("alice"));
    }

    @Test
    void shouldStillForceOnlineModeForEnrolledPremiumPlayerWhenPendingAttemptFails() {
        Set<String> pending = new HashSet<>(Set.of("alice"));
        Predicate<String> requiresVerification = name -> "alice".equals(name) || pending.contains(name);
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), requiresVerification, pending::contains, pending::remove, () -> false);

        manager.onPreLogin(new PreLoginEvent(mock(InboundConnection.class), "Alice", null));
        PreLoginEvent secondAttempt = new PreLoginEvent(mock(InboundConnection.class), "Alice", null);
        manager.onPreLogin(secondAttempt);

        assertEquals(PreLoginEvent.PreLoginComponentResult.forceOnlineMode().toString(),
            secondAttempt.getResult().toString());
        assertFalse(pending.contains("alice"));
    }

    @Test
    void shouldResetAttemptWhenPendingVerificationIsRearmed() {
        Set<String> pending = new HashSet<>(Set.of("alice"));
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), pending::contains, pending::contains, pending::remove, () -> false);

        manager.onPreLogin(new PreLoginEvent(mock(InboundConnection.class), "Alice", null));
        // A new /premium run sends premium.pending.set again, which clears the verification state
        manager.clearVerifiedPremium("alice");
        PreLoginEvent reconnect = new PreLoginEvent(mock(InboundConnection.class), "Alice", null);
        manager.onPreLogin(reconnect);

        assertEquals(PreLoginEvent.PreLoginComponentResult.forceOnlineMode().toString(),
            reconnect.getResult().toString());
        assertTrue(pending.contains("alice"));
    }
}
