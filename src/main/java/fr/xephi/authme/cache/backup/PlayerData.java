package fr.xephi.authme.cache.backup;

public class PlayerData {

    private final String group;
    private final boolean operator;
    private final boolean flyEnabled;

    public PlayerData(String group, boolean operator, boolean flyEnabled) {
        this.group = group;
        this.operator = operator;
        this.flyEnabled = flyEnabled;
    }

    public String getGroup() {
        return group;
    }

    public boolean getOperator() {
        return operator;
    }

    public boolean isFlyEnabled() {
        return flyEnabled;
    }
}
