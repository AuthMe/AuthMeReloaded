package fr.xephi.authme.data.limbo;

import fr.xephi.authme.task.MessageTask;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collection;

/**
 * Represents a player which is not logged in and keeps track of certain states (like OP status, flying)
 * which may be revoked from the player until he has logged in or registered.
 */
public class LimboPlayer {

    public static final float DEFAULT_WALK_SPEED = 0.2f;
    public static final float DEFAULT_FLY_SPEED = 0.1f;

    private final boolean canFly;
    private final boolean operator;
    private final Collection<String> groups;
    private final Location loc;
    private final float walkSpeed;
    private final float flySpeed;
    private BukkitTask timeoutTask = null;
    private MessageTask messageTask = null;
    private LimboPlayerState state = LimboPlayerState.PASSWORD_REQUIRED;

    public LimboPlayer(Location loc, boolean operator, Collection<String> groups, boolean fly, float walkSpeed,
                       float flySpeed) {
        this.loc = loc;
        this.operator = operator;
        this.groups = groups;
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
    public Collection<String> getGroups() {
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
    public BukkitTask getTimeoutTask() {
        return timeoutTask;
    }

    /**
     * Set the timeout task of the player. The timeout task kicks the player after a configurable
     * amount of time if he hasn't logged in or registered.
     *
     * @param timeoutTask The task to set
     */
    public void setTimeoutTask(BukkitTask timeoutTask) {
        if (this.timeoutTask != null) {
            this.timeoutTask.cancel();
        }
        this.timeoutTask = timeoutTask;
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
    public void setMessageTask(MessageTask messageTask) {
        if (this.messageTask != null) {
            this.messageTask.cancel();
        }
        this.messageTask = messageTask;
    }

    /**
     * Clears all tasks associated to the player.
     */
    public void clearTasks() {
        setMessageTask(null);
        setTimeoutTask(null);
    }

    public LimboPlayerState getState() {
        return state;
    }

    public void setState(LimboPlayerState state) {
        this.state = state;
    }
}
