package fr.xephi.authme.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class BungeeAuthenticationStore {

    private final Set<String> authenticatedPlayers = ConcurrentHashMap.newKeySet();

    void markAuthenticated(String playerName) {
        authenticatedPlayers.add(normalizeName(playerName));
    }

    void markLoggedOut(String playerName) {
        authenticatedPlayers.remove(normalizeName(playerName));
    }

    boolean isAuthenticated(ProxiedPlayer player) {
        return isAuthenticated(player.getName());
    }

    boolean isAuthenticated(String playerName) {
        return authenticatedPlayers.contains(normalizeName(playerName));
    }

    void clear(ProxiedPlayer player) {
        markLoggedOut(player.getName());
    }

    private static String normalizeName(String playerName) {
        return playerName.toLowerCase(Locale.ROOT);
    }
}
