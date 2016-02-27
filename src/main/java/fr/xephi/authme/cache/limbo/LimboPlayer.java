package fr.xephi.authme.cache.limbo;

import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class LimboPlayer {

    private final String name;
    private final boolean fly;
    private Location loc = null;
    private BukkitTask timeoutTaskId = null;
    private BukkitTask messageTaskId = null;
    private boolean operator = false;
    private String group;

    public LimboPlayer(String name, Location loc, boolean operator,
                       String group, boolean fly) {
        this.name = name;
        this.loc = loc;
        this.operator = operator;
        this.group = group;
        this.fly = fly;
    }

    /**
     * Method getName.
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Method getLoc.
     *
     * @return Location
     */
    public Location getLoc() {
        return loc;
    }

    /**
     * Method getOperator.
     *
     * @return boolean
     */
    public boolean getOperator() {
        return operator;
    }

    /**
     * Method getGroup.
     *
     * @return String
     */
    public String getGroup() {
        return group;
    }

    public boolean isFly() {
        return fly;
    }

    public BukkitTask getTimeoutTaskId() {
        return timeoutTaskId;
    }

    /**
     * Method setTimeoutTaskId.
     *
     * @param i BukkitTask
     */
    public void setTimeoutTaskId(BukkitTask i) {
        if (this.timeoutTaskId != null) {
            this.timeoutTaskId.cancel();
        }
        this.timeoutTaskId = i;
    }

    /**
     * Method getMessageTaskId.
     *
     * @return BukkitTask
     */
    public BukkitTask getMessageTaskId() {
        return messageTaskId;
    }

    /**
     * Method setMessageTaskId.
     *
     * @param messageTaskId BukkitTask
     */
    public void setMessageTaskId(BukkitTask messageTaskId) {
        if (this.messageTaskId != null) {
            this.messageTaskId.cancel();
        }
        this.messageTaskId = messageTaskId;
    }

    /**
     * Method clearTask.
     */
    public void clearTask() {
        if (messageTaskId != null) {
            messageTaskId.cancel();
        }
        messageTaskId = null;
        if (timeoutTaskId != null) {
            timeoutTaskId.cancel();
        }
        timeoutTaskId = null;
    }
}
