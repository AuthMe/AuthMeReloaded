package fr.xephi.authme.permission.handlers;

import net.luckperms.api.context.ImmutableContextSet;
import net.luckperms.api.model.group.Group;

public class LuckPermGroup {
    private Group group;
    private ImmutableContextSet contexts;

    public LuckPermGroup(Group group, ImmutableContextSet contexts) {
        this.group = group;

        this.contexts = contexts;
    }

    public Group getGroup() {
        return group;
    }

    public ImmutableContextSet getContexts() {
        return contexts;
    }
}
