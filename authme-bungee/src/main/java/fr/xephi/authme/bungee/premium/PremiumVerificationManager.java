package fr.xephi.authme.bungee.premium;

import java.util.UUID;

/**
 * Proxy-side premium verification. Implementations that use PacketEvents are only instantiated
 * (and therefore only loaded) when the PacketEvents plugin is installed on the proxy.
 */
public interface PremiumVerificationManager {

    /**
     * Registers the premium verification listeners.
     */
    void register();

    /**
     * Re-evaluates whether the premium verification listeners should be active.
     */
    void refreshRegistration();

    /**
     * @param normalizedName lower-case player name
     * @return the verified premium UUID, or {@code null} if the player was not verified
     */
    UUID getVerifiedPremiumUuid(String normalizedName);

    /**
     * Discards the verification result of the given player.
     *
     * @param normalizedName lower-case player name
     */
    void clearVerifiedPremium(String normalizedName);

    /**
     * Unregisters all listeners and releases resources.
     */
    void shutdown();
}
