package fr.xephi.authme.velocity;

import com.velocitypowered.api.proxy.Player;

import java.util.IdentityHashMap;
import java.util.Map;

final class VelocityAuthenticationStore {

    private final Map<Player, String> authenticatedPlayers = new IdentityHashMap<>();

    synchronized void markAuthenticated(Player player) {
        authenticatedPlayers.put(player, player.getUsername());
    }

    synchronized void markLoggedOut(Player player) {
        authenticatedPlayers.remove(player);
    }

    synchronized boolean isAuthenticated(Player player) {
        return authenticatedPlayers.containsKey(player);
    }

    void clear(Player player) {
        markLoggedOut(player);
    }
}
