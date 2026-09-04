package fr.xephi.authme.bungee.events;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

public class AuthMeBungeeLogoutEvent extends Event {

    private final ProxiedPlayer player;

    public AuthMeBungeeLogoutEvent(ProxiedPlayer player) {
        this.player = player;
    }

    /**
     * Return the player concerned by this event.
     *
     * @return The player who logged out correctly in the backend
     */
    public ProxiedPlayer getPlayer() {
        return player;
    }
}
