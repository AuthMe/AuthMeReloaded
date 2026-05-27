package fr.xephi.authme.bungee.premium;

import com.github.retrooper.packetevents.PacketEvents;
import net.md_5.bungee.api.ProxyServer;

import java.util.UUID;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

public final class BungeePremiumVerificationManager {

    private final ProxyServer proxyServer;
    private final Logger logger;
    private final Predicate<String> requiresVerification;
    private final Predicate<String> isPendingVerification;
    private final Consumer<String> pendingVerificationFailureHandler;
    private final BooleanSupplier keepOfflineUuidCompatibility;
    private final ProxyPremiumLoginVerifier loginVerifier;
    private ProxyPremiumVerificationPacketListener packetListener;
    private boolean registered;

    public BungeePremiumVerificationManager(ProxyServer proxyServer, Logger logger,
                                            Predicate<String> requiresVerification,
                                            Predicate<String> isPendingVerification,
                                            Consumer<String> pendingVerificationFailureHandler,
                                            BooleanSupplier keepOfflineUuidCompatibility) {
        this.proxyServer = proxyServer;
        this.logger = logger;
        this.requiresVerification = requiresVerification;
        this.isPendingVerification = isPendingVerification;
        this.pendingVerificationFailureHandler = pendingVerificationFailureHandler;
        this.keepOfflineUuidCompatibility = keepOfflineUuidCompatibility;
        this.loginVerifier = new ProxyPremiumLoginVerifier("authme-bungee-premium", this.logger::warning);
    }

    public void register() {
        refreshRegistration();
    }

    public void refreshRegistration() {
        if (!keepOfflineUuidCompatibility.getAsBoolean()) {
            unregisterPacketListener();
            return;
        }
        if (registered) {
            return;
        }
        if (proxyServer.getPluginManager().getPlugin("packetevents") == null) {
            logger.warning("PacketEvents is not loaded on the proxy; premium proxy verification stays disabled");
            return;
        }
        packetListener = new ProxyPremiumVerificationPacketListener(
            requiresVerification, isPendingVerification, pendingVerificationFailureHandler, loginVerifier, logger::warning);
        PacketEvents.getAPI().getEventManager().registerListener(packetListener);
        registered = true;
        logger.info("Registered PacketEvents premium verification on the Bungee proxy");
    }

    public UUID getVerifiedPremiumUuid(String normalizedName) {
        return loginVerifier.getVerifiedUuid(normalizedName);
    }

    public void clearVerifiedPremium(String normalizedName) {
        loginVerifier.clearVerified(normalizedName);
    }

    public void shutdown() {
        unregisterPacketListener();
        loginVerifier.shutdown();
    }

    private void unregisterPacketListener() {
        if (registered && packetListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(packetListener);
            packetListener = null;
            registered = false;
        }
    }
}
