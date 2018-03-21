package fr.xephi.authme;

import java.util.UUID;

public class OfflinePlayerWrapper {

    private final UUID uniqueId;
    private final String name;

    public OfflinePlayerWrapper(UUID uniqueId, String name) {
        this.uniqueId = uniqueId;
        this.name = name;
    }

    public UUID getUniqueId() {
        return uniqueId;
    }

    public String getName() {
        return name;
    }
}
