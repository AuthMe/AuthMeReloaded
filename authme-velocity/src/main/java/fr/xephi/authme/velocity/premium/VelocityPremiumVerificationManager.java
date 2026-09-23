package fr.xephi.authme.velocity.premium;

import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.GameProfileRequestEvent;
import com.velocitypowered.api.util.UuidUtils;
import org.slf4j.Logger;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class VelocityPremiumVerificationManager {

    private final Logger logger;
    private final Predicate<String> requiresVerification;
    private final Predicate<String> isPendingVerification;
    private final Consumer<String> pendingVerificationFailureHandler;
    private final BooleanSupplier keepOfflineUuidCompatibility;
    private final Set<String> pendingVerificationAttempted = ConcurrentHashMap.newKeySet();
    private final ProxyPremiumLoginVerifier loginVerifier;
    private boolean registered;

    public VelocityPremiumVerificationManager(Logger logger,
                                              Predicate<String> requiresVerification,
                                              Predicate<String> isPendingVerification,
                                              Consumer<String> pendingVerificationFailureHandler,
                                              BooleanSupplier keepOfflineUuidCompatibility) {
        this.logger = logger;
        this.requiresVerification = requiresVerification;
        this.isPendingVerification = isPendingVerification;
        this.pendingVerificationFailureHandler = pendingVerificationFailureHandler;
        this.keepOfflineUuidCompatibility = keepOfflineUuidCompatibility;
        this.loginVerifier = new ProxyPremiumLoginVerifier("authme-velocity-premium",
            message -> this.logger.warn(message));
    }

    public void register() {
        if (registered) {
            return;
        }
        registered = true;
        logger.info("Registered native Velocity premium verification");
    }

    public void onPreLogin(PreLoginEvent event) {
        String normalizedName = normalize(event.getUsername());
        if (isPendingVerification.test(normalizedName) && !pendingVerificationAttempted.add(normalizedName)) {
            // Velocity fires no event when Mojang rejects a forced online-mode login, so a pending
            // player showing up again means the previous attempt failed: cancel the enrollment and
            // let them log in with their password instead of forcing online mode forever.
            pendingVerificationAttempted.remove(normalizedName);
            pendingVerificationFailureHandler.accept(normalizedName);
        }
        if (requiresVerification.test(normalizedName)) {
            event.setResult(PreLoginEvent.PreLoginComponentResult.forceOnlineMode());
        }
    }

    public void onGameProfileRequest(GameProfileRequestEvent event) {
        String normalizedName = normalize(event.getUsername());
        if (!requiresVerification.test(normalizedName) || !event.isOnlineMode()) {
            return;
        }

        UUID verifiedPremiumUuid = event.getOriginalProfile().getId();
        loginVerifier.storeVerified(normalizedName, verifiedPremiumUuid);
        pendingVerificationAttempted.remove(normalizedName);
        boolean rewrite = keepOfflineUuidCompatibility.getAsBoolean();
        logger.info("onGameProfileRequest: stored verified premium UUID {} for '{}' (keepOfflineUuidCompatibility={})",
            verifiedPremiumUuid, normalizedName, rewrite);
        if (rewrite) {
            event.setGameProfile(event.getGameProfile().withId(UuidUtils.generateOfflinePlayerUuid(event.getUsername())));
        }

        if (isPendingVerification.test(normalizedName)) {
            logger.info("Premium enrollment for '{}' was verified on the Velocity proxy", normalizedName);
        } else {
            logger.debug("Verified premium login for '{}' on the Velocity proxy", normalizedName);
        }
    }

    public UUID getVerifiedPremiumUuid(String normalizedName) {
        return loginVerifier.getVerifiedUuid(normalizedName);
    }

    public void clearVerifiedPremium(String normalizedName) {
        loginVerifier.clearVerified(normalizedName);
        pendingVerificationAttempted.remove(normalizedName);
    }

    public void shutdown() {
        if (!registered) {
            return;
        }
        registered = false;
        loginVerifier.shutdown();
    }

    private static String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
