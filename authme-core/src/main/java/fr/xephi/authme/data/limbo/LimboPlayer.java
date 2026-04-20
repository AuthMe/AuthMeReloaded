package fr.xephi.authme.data.limbo;

import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.service.CancellableTask;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player which is not logged in and keeps track of certain states (like OP status, flying)
 * which may be revoked from the player until he has logged in or registered.
 */
public class LimboPlayer {

    public static final float DEFAULT_WALK_SPEED = 0.2f;
    public static final float DEFAULT_FLY_SPEED = 0.1f;

    private final boolean canFly;
    private final boolean operator;
    private final Collection<UserGroup> groups;
    private final Location loc;
    private final float walkSpeed;
    private final float flySpeed;
    private CancellableTask timeoutTask = null;
    private MessageTask messageTask = null;
    private CancellableTask messageTaskHandle = null;
    private LimboPlayerState state = LimboPlayerState.PASSWORD_REQUIRED;
    private Set<UUID> enderPearlUuids = new HashSet<>();
    private UUID vehicleUuid;
    private EntityType vehicleType;

    public LimboPlayer(Location loc, boolean operator, Collection<UserGroup> groups, boolean fly, float walkSpeed,
                       float flySpeed) {
        this.loc = loc;
        this.operator = operator;
        this.groups = new ArrayList<>(groups); // prevent bug #2413
        this.canFly = fly;
        this.walkSpeed = walkSpeed;
        this.flySpeed = flySpeed;
    }

    /**
     * Return the player's original location.
     *
     * @return The player's location
     */
    public Location getLocation() {
        return loc;
    }

    /**
     * Return whether the player is an operator or not (i.e. whether he is an OP).
     *
     * @return True if the player has OP status, false otherwise
     */
    public boolean isOperator() {
        return operator;
    }

    /**
     * Return the player's permissions groups.
     *
     * @return The permissions groups the player belongs to
     */
    public Collection<UserGroup> getGroups() {
        return groups;
    }

    public boolean isCanFly() {
        return canFly;
    }

    public float getWalkSpeed() {
        return walkSpeed;
    }

    public float getFlySpeed() {
        return flySpeed;
    }

    /**
     * Return the timeout task, which kicks the player if he hasn't registered or logged in
     * after a configurable amount of time.
     *
     * @return The timeout task associated to the player
     */
    public CancellableTask getTimeoutTask() {
        return timeoutTask;
    }

    /**
     * Set the timeout task of the player. The timeout task kicks the player after a configurable
     * amount of time if he hasn't logged in or registered.
     *
     * @param timeoutTask The task to set
     */
    public void setTimeoutTask(CancellableTask timeoutTask) {
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }
        this.timeoutTask = timeoutTask;
    }

    public void setTimeoutTask(BukkitTask timeoutTask) {
        setTimeoutTask(timeoutTask == null ? null : timeoutTask::cancel);
    }

    /**
     * Return the message task reminding the player to log in or register.
     *
     * @return The task responsible for sending the message regularly
     */
    public MessageTask getMessageTask() {
        return messageTask;
    }

    /**
     * Set the messages task responsible for telling the player to log in or register.
     *
     * @param messageTask The message task to set
     */
    public void setMessageTask(MessageTask messageTask, CancellableTask messageTaskHandle) {
        if (this.messageTaskHandle != null) {
            this.messageTaskHandle.cancel();
        }
        this.messageTask = messageTask;
        this.messageTaskHandle = messageTaskHandle;
    }

    public void setMessageTask(MessageTask messageTask) {
        setMessageTask(messageTask, messageTask == null ? null : messageTask::cancel);
    }

    /**
     * Clears all tasks associated to the player.
     */
    public void clearTasks() {
        setMessageTask(null, null);
        setTimeoutTask((CancellableTask) null);
    }

    public LimboPlayerState getState() {
        return state;
    }

    public void setState(LimboPlayerState state) {
        this.state = state;
    }

    /**
     * Returns the entity UUIDs of ender pearls in flight for this player,
     * used to restore stasis chambers after a reconnect.
     *
     * @return mutable set of ender pearl entity UUIDs (never null)
     */
    public Set<UUID> getEnderPearlUuids() {
        return enderPearlUuids;
    }

    /**
     * Sets the ender pearl entity UUIDs to restore on reconnect.
     *
     * @param pearlUuids the set of pearl entity UUIDs to track
     */
    public void setEnderPearlUuids(Set<UUID> pearlUuids) {
        this.enderPearlUuids = new HashSet<>(pearlUuids);
    }

    /**
     * Returns the UUID of the vehicle entity the player was riding when they disconnected,
     * used to remount them on reconnect.
     *
     * @return the vehicle entity UUID, or null if not riding anything
     */
    public UUID getVehicleUuid() {
        return vehicleUuid;
    }

    /**
     * Returns the type of the vehicle the player was riding when they disconnected.
     *
     * @return the vehicle EntityType, or null if not riding anything
     */
    public EntityType getVehicleType() {
        return vehicleType;
    }

    /**
     * Sets the vehicle to restore on reconnect. Pass null for both arguments to clear.
     *
     * @param vehicleUuid the entity UUID of the vehicle, or null
     * @param vehicleType the entity type of the vehicle, or null
     */
    public void setVehicle(UUID vehicleUuid, EntityType vehicleType) {
        this.vehicleUuid = vehicleUuid;
        this.vehicleType = vehicleType;
    }
}
