package fr.xephi.authme.data.limbo;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.UUID;

/**
 * Snapshot of an in-flight ender pearl so it can be re-associated or recreated after reconnect.
 */
public final class EnderPearlRestoreData {

    private final UUID uuid;
    private final Location location;
    private final Vector velocity;

    public EnderPearlRestoreData(UUID uuid, Location location, Vector velocity) {
        this.uuid = Objects.requireNonNull(uuid, "uuid");
        this.location = location;
        this.velocity = velocity;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Location getLocation() {
        return location;
    }

    public Vector getVelocity() {
        return velocity;
    }

    public boolean hasDetailedState() {
        return location != null || velocity != null;
    }

    public boolean canBeRecreated() {
        return location != null && location.getWorld() != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EnderPearlRestoreData)) {
            return false;
        }
        EnderPearlRestoreData that = (EnderPearlRestoreData) o;
        return Objects.equals(uuid, that.uuid)
            && Objects.equals(location, that.location)
            && Objects.equals(velocity, that.velocity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uuid, location, velocity);
    }
}
