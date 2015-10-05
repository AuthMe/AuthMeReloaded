package fr.xephi.authme.cache.backup;

public class DataFileCache {

    private String group;
    private boolean operator;
    private boolean flying;

    public DataFileCache(String group, boolean operator, boolean flying) {
        this.group = group;
        this.operator = operator;
        this.flying = flying;
    }

    public String getGroup() {
        return group;
    }

    public boolean getOperator() {
        return operator;
    }

    public boolean isFlying() {
        return flying;
    }
}
