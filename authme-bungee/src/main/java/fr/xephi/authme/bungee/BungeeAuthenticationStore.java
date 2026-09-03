package fr.xephi.authme.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.IdentityHashMap;
import java.util.Map;

final class BungeeAuthenticationStore {

    private final Map<ProxiedPlayer, String> authenticatedPlayers = new IdentityHashMap<>();

    synchronized void markAuthenticated(ProxiedPlayer player) {
        authenticatedPlayers.put(player, player.getName());
    }

    synchronized void markLoggedOut(ProxiedPlayer player) {
        authenticatedPlayers.remove(player);
    }

    synchronized boolean isAuthenticated(ProxiedPlayer player) {
        return authenticatedPlayers.containsKey(player);
    }

    void clear(ProxiedPlayer player) {
        markLoggedOut(player);
    }
}
