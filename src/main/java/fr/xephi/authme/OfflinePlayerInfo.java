package fr.xephi.authme;

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

}
