package fr.xephi.authme.bungee.premium;

import net.md_5.bungee.api.connection.PendingConnection;

import java.util.Locale;
import java.util.function.Predicate;

public final class BungeePremiumOnlineModeHandler {

    private final Predicate<String> requiresVerification;

    public BungeePremiumOnlineModeHandler(Predicate<String> requiresVerification) {
        this.requiresVerification = requiresVerification;
    }

    public void enableOnlineModeIfRequired(PendingConnection connection) {
        String username = connection.getName();
        if (username == null) {
            return;
        }

        String normalizedName = username.toLowerCase(Locale.ROOT);
        if (requiresVerification.test(normalizedName) && !connection.isOnlineMode()) {
            connection.setOnlineMode(true);
        }
    }
}
