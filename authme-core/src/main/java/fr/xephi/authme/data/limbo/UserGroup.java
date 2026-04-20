package fr.xephi.authme.data.limbo;

import java.util.Map;
import java.util.Objects;

public class UserGroup {

    private String groupName;
    private Map<String, String> contextMap;

    public UserGroup(String groupName) {
        this.groupName = groupName;
    }

    public UserGroup(String groupName, Map<String, String> contextMap) {
        this.groupName = groupName;
        this.contextMap = contextMap;
    }

    public String getGroupName() {
        return groupName;
    }

    public Map<String, String> getContextMap() {
        return contextMap;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UserGroup userGroup = (UserGroup) o;
        return Objects.equals(groupName, userGroup.groupName)
            && Objects.equals(contextMap, userGroup.contextMap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupName, contextMap);
    }
}
