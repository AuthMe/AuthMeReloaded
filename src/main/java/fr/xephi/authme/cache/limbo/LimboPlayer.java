package fr.xephi.authme.cache.limbo;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class LimboPlayer {

    private String name;
    private Location loc = null;
    private BukkitTask timeoutTaskId = null;
    private BukkitTask messageTaskId = null;
    private GameMode gameMode = GameMode.SURVIVAL;
    private boolean operator = false;
    private String group = "";
    private boolean flying = false;

    /**
     * Constructor for LimboPlayer.
     *
     * @param name     String
     * @param loc      Location
     * @param gameMode GameMode
     * @param operator boolean
     * @param group    String
     * @param flying   boolean
     */
    public LimboPlayer(String name, Location loc, GameMode gameMode,
                       boolean operator, String group, boolean flying) {
        this.name = name;
        this.loc = loc;
        this.gameMode = gameMode;
        this.operator = operator;
        this.group = group;
        this.flying = flying;
    }

    /**
     * Constructor for LimboPlayer.
     *
     * @param name  String
     * @param group String
     */
    public LimboPlayer(String name, String group) {
        this.name = name;
        this.group = group;
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
     * Method getGameMode.
     *
     * @return GameMode
     */
    public GameMode getGameMode() {
        return gameMode;
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

    /**
     * Method getTimeoutTaskId.
     *
     * @return BukkitTask
     */
    public BukkitTask getTimeoutTaskId() {
        return timeoutTaskId;
    }

    /**
     * Method setTimeoutTaskId.
     *
     * @param i BukkitTask
     */
    public void setTimeoutTaskId(BukkitTask i) {
        if (this.timeoutTaskId != null)
            this.timeoutTaskId.cancel();
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
        if (this.messageTaskId != null)
            this.messageTaskId.cancel();
        this.messageTaskId = messageTaskId;
    }

    /**
     * Method isFlying.
     *
     * @return boolean
     */
    public boolean isFlying() {
        return flying;
    }
}
