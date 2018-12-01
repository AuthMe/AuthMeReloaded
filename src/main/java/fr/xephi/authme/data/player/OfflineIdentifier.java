package fr.xephi.authme.data.player;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Optional;

public class OfflineIdentifier extends NamedIdentifier {

    private final OfflinePlayer player;

    public OfflineIdentifier(OfflinePlayer player) {
        super(player.getName(), player.getName().toLowerCase());
        this.player = player;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public Optional<Player> getOnlinePlayer() {
        return Optional.ofNullable(player.getPlayer());
    }
}
