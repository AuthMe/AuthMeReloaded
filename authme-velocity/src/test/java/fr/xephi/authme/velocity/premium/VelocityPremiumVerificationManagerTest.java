package fr.xephi.authme.velocity.premium;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.proxy.InboundConnection;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.UuidUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

class VelocityPremiumVerificationManagerTest {

    @Test
    void shouldForceOnlineModeForPremiumUser() {
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), "alice"::equals, normalizedName -> false, () -> false);

        PreLoginEvent event = new PreLoginEvent(mock(InboundConnection.class), "Alice", null);
        manager.onPreLogin(event);

        assertEquals(PreLoginEvent.PreLoginComponentResult.forceOnlineMode().toString(),
            event.getResult().toString());
    }

    @Test
    void shouldRewriteVerifiedProfileToOfflineUuid() {
        VelocityPremiumVerificationManager manager = new VelocityPremiumVerificationManager(
            mock(Logger.class), "alice"::equals, normalizedName -> false, () -> true);
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
            mock(Logger.class), "alice"::equals, normalizedName -> false, () -> true);
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
            mock(Logger.class), "alice"::equals, normalizedName -> false, () -> false);
        UUID mojangUuid = UUID.fromString("8d6d0684-d8b4-4d40-8d2d-0dd4df5555c8");
        GameProfile originalProfile = new GameProfile(mojangUuid, "Alice", List.of());
        GameProfileRequestEvent event = new GameProfileRequestEvent(
            mock(InboundConnection.class), originalProfile, true);

        manager.onGameProfileRequest(event);

        assertEquals(mojangUuid, event.getGameProfile().getId());
        assertEquals(mojangUuid, manager.getVerifiedPremiumUuid("alice"));
    }
}
