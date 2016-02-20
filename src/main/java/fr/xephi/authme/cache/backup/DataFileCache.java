package fr.xephi.authme.cache.backup;

/**
 */
public class DataFileCache {

    private final String group;
    private final boolean operator;
    private final boolean flyEnabled;

    /**
     * Constructor for DataFileCache.
     *
     * @param group    String
     * @param operator boolean
     */
    public DataFileCache(String group, boolean operator, boolean flyEnabled) {
        this.group = group;
        this.operator = operator;
        this.flyEnabled = flyEnabled;
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

    public boolean isFlyEnabled() {
        return flyEnabled;
    }
}
