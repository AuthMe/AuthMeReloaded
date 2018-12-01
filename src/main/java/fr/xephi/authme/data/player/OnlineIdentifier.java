package fr.xephi.authme.data.player;

import org.bukkit.entity.Player;

public class OnlineIdentifier extends OfflineIdentifier {

    public OnlineIdentifier(Player player) {
        super(player);
    }

    public Player getPlayer() {
        return (Player) super.getPlayer();
    }
}
