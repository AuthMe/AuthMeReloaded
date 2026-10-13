package fr.xephi.authme.bungee.premium;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProxyPremiumVerificationPacketListenerTest {

    private static final UUID MOJANG_UUID = UUID.fromString("8d6d0684-d8b4-4d40-8d2d-0dd4df5555c8");

    private final ProxyPremiumLoginVerifier loginVerifier = new ProxyPremiumLoginVerifier("test", message -> { });

    @AfterEach
    void shutDownVerifier() {
        loginVerifier.shutdown();
    }

    @Test
    void shouldStoreVerificationOfNameNotConnected() {
        ProxyPremiumVerificationPacketListener listener = createListener(false);

        listener.onVerificationCompleted("notch", Optional.of(MOJANG_UUID), false);

        assertEquals(MOJANG_UUID, loginVerifier.getVerifiedUuid("notch"));
    }

    @Test
    void shouldDiscardVerificationWhenPlayerWithNameIsAlreadyConnected() {
        ProxyPremiumVerificationPacketListener listener = createListener(true);

        listener.onVerificationCompleted("notch", Optional.of(MOJANG_UUID), false);

        assertNull(loginVerifier.getVerifiedUuid("notch"));
    }

    private ProxyPremiumVerificationPacketListener createListener(boolean nameConnected) {
        return new ProxyPremiumVerificationPacketListener(name -> true, name -> false, name -> { },
            loginVerifier, name -> nameConnected, message -> { });
    }
}
