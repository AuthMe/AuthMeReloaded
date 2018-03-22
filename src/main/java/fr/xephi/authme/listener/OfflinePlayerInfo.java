package fr.xephi.authme.listener;

import fr.xephi.authme.util.Utils;
import org.bukkit.OfflinePlayer;

import java.util.Optional;
import java.util.UUID;

public class OfflinePlayerInfo {

    private final String name;
    private final Optional<UUID> uniqueId;

    public OfflinePlayerInfo(String name, Optional<UUID> uniqueId) {
        this.name = name;
        this.uniqueId = uniqueId;
    }

    public String getName() {
        return name;
    }

    public Optional<UUID> getUniqueId() {
        return uniqueId;
    }

    public static OfflinePlayerInfo fromPlayer(OfflinePlayer player) {
        return new OfflinePlayerInfo(player.getName(),
            Utils.hasUniqueIdSupport() ? Optional.of(player.getUniqueId()) : Optional.empty()
        );
    }

}
