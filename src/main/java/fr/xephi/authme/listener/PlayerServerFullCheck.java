package fr.xephi.authme.listener;

import com.destroystokyo.paper.profile.PlayerProfile;
import net.kyori.adventure.text.Component;

public class PlayerServerFullCheck {

    private final PlayerProfile profile;
    private Component kickMessage;
    private boolean allow;

    public PlayerServerFullCheck(PlayerProfile profile, Component kickMessage, boolean allow) {
        this.profile = profile;
        this.kickMessage = kickMessage;
        this.allow = allow;
    }

    public Component getKickMessage() {
        return this.kickMessage;
    }

    public void deny(Component kickMessage) {
        this.kickMessage = kickMessage;
        this.allow = false;
    }

    public PlayerProfile getPlayerProfile() {
        return this.profile;
    }

    public void allow() {
        this.allow = true;
    }

    public boolean isAllowed() {
        return this.allow;
    }
}
