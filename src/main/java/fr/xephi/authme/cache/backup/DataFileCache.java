package fr.xephi.authme.cache.backup;

/**
 */
public class DataFileCache {

    private final String group;
    private final boolean operator;
    private final boolean flying;

    /**
     * Constructor for DataFileCache.
     *
     * @param group    String
     * @param operator boolean
     * @param flying   boolean
     */
    public DataFileCache(String group, boolean operator, boolean flying) {
        this.group = group;
        this.operator = operator;
        this.flying = flying;
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
     * Method getOperator.
     *
     * @return boolean
     */
    public boolean getOperator() {
        return operator;
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
