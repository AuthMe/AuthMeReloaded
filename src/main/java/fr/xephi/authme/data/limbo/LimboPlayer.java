package fr.xephi.authme.data.limbo;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

/**
 * Represents a player which is not logged in and keeps track of certain states (like OP status, flying)
 * which may be revoked from the player until he has logged in or registered.
 */
public class LimboPlayer {

    private final boolean canFly;
    private final boolean operator;
    private final String group;
    private final Location loc;
    private final float walkSpeed;
    private final float flySpeed;
    private BukkitTask timeoutTask = null;
    private BukkitTask messageTask = null;

    public LimboPlayer(Location loc, boolean operator, String group, boolean fly, float walkSpeed, float flySpeed) {
        this.loc = loc;
        this.operator = operator;
        this.group = group;
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
     * Return the player's permissions group.
     *
     * @return The permissions group the player belongs to
     */
    public String getGroup() {
        return group;
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
    public BukkitTask getMessageTask() {
        return messageTask;
    }

    /**
     * Set the messages task responsible for telling the player to log in or register.
     *
     * @param messageTask The message task to set
     */
    public void setMessageTask(BukkitTask messageTask) {
        if (this.messageTask != null) {
            this.messageTask.cancel();
        }
        this.messageTask = messageTask;
    }

    /**
     * Clears all tasks associated to the player.
     */
    public void clearTasks() {
        if (messageTask != null) {
            messageTask.cancel();
        }
        messageTask = null;
        if (timeoutTask != null) {
            timeoutTask.cancel();
        }
        timeoutTask = null;
    }
}
