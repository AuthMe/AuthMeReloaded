package fr.xephi.authme.velocity;

import com.velocitypowered.api.proxy.Player;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class VelocityAuthenticationStore {

    private final Set<String> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    void markAuthenticated(String playerName) {
        authenticatedPlayers.add(normalizeName(playerName));
    }

    void markLoggedOut(String playerName) {
        authenticatedPlayers.remove(normalizeName(playerName));
    }

    boolean isAuthenticated(Player player) {
        return isAuthenticated(player.getUsername());
    }

    boolean isAuthenticated(String playerName) {
        return authenticatedPlayers.contains(normalizeName(playerName));
    }

    void clear(Player player) {
        markLoggedOut(player.getUsername());
    }

    private static String normalizeName(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }
}
