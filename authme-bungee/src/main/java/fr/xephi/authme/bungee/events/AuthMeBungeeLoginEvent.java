package fr.xephi.authme.bungee.events;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Event;

public class AuthMeBungeeLoginEvent extends Event {

    private final ProxiedPlayer player;

    private final boolean premium;

    public AuthMeBungeeLoginEvent(ProxiedPlayer player, boolean premium) {
        this.player = player;
        this.premium = premium;
    }

    /**
     * Return whether this player required Premium verification for login
     *
     * @return if the player required premium verification
     */
    public boolean isPremium() {
        return premium;
    }

    /**
     * Return the player concerned by this event.
     *
     * @return The player who logged in correctly in the backend
     */
    public ProxiedPlayer getPlayer() {
        return player;
    }
}
