package fr.xephi.authme.bungee.premium;

import java.util.UUID;

/**
 * No-op implementation used when PacketEvents is not available on the proxy: the AuthMeBungee
 * plugin keeps working, but players are never marked as proxy-verified premium players.
 */
final class DisabledPremiumVerificationManager implements PremiumVerificationManager {

    @Override
    public void register() {
    }

    @Override
    public void refreshRegistration() {
    }

    @Override
    public UUID getVerifiedPremiumUuid(String normalizedName) {
        return null;
    }

    @Override
    public void clearVerifiedPremium(String normalizedName) {
    }

    @Override
    public void shutdown() {
    }
}
