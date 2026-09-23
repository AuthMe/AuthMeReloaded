package fr.xephi.authme.bungee.premium;

import net.md_5.bungee.api.ProxyServer;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Creates the {@link PremiumVerificationManager} for the Bungee proxy. The PacketEvents-backed
 * implementation is only instantiated when PacketEvents is installed, so that classes referencing
 * the PacketEvents library are never loaded on proxies without it.
 */
public final class PremiumVerificationManagers {

    private static final String PACKET_EVENTS_PLUGIN_NAME = "packetevents";

    private PremiumVerificationManagers() {
    }

    public static PremiumVerificationManager create(ProxyServer proxyServer, Logger logger,
                                                    Predicate<String> requiresVerification,
                                                    Predicate<String> isPendingVerification,
                                                    Consumer<String> pendingVerificationFailureHandler,
                                                    BooleanSupplier keepOfflineUuidCompatibility) {
        if (proxyServer.getPluginManager().getPlugin(PACKET_EVENTS_PLUGIN_NAME) == null) {
            logger.warning("PacketEvents is not installed on the proxy; premium proxy verification stays disabled");
            return new DisabledPremiumVerificationManager();
        }
        try {
            return new BungeePremiumVerificationManager(proxyServer, logger, requiresVerification,
                isPendingVerification, pendingVerificationFailureHandler, keepOfflineUuidCompatibility);
        } catch (LinkageError e) {
            logger.warning("PacketEvents is installed but its classes could not be loaded; premium proxy "
                + "verification stays disabled: " + e);
            return new DisabledPremiumVerificationManager();
        }
    }
}
