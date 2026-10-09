package fr.xephi.authme.velocity.events;

import com.velocitypowered.api.proxy.Player;

public class AuthMeVelocityLogoutEvent {

    private final Player player;

    public AuthMeVelocityLogoutEvent(Player player) {
        this.player = player;
    }

    /**
     * Return the player concerned by this event.
     *
     * @return The player who logged out correctly in the backend
     */
    public Player getPlayer() {
        return player;
    }
}
