package fr.xephi.authme.velocity.events;

import com.velocitypowered.api.proxy.Player;

public class AuthMeVelocityLoginEvent {


    private final Player player;
    private final boolean premium;

    public AuthMeVelocityLoginEvent(Player player, boolean premium) {
        this.player = player;
        this.premium = premium;
    }

    /**
     * Return the player concerned by this event.
     *
     * @return The player who logged in correctly in the backend
     */
    public Player getPlayer() {
        return player;
    }


    /**
     * Return whether this player required Premium verification for login
     *
     * @return if the player required premium verification
     */
    public boolean isPremium() {
        return premium;
    }
}
