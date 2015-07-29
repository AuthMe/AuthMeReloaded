package fr.xephi.authme.modules;

import fr.xephi.authme.AuthMe;

public interface Module {

    public String getName();

    public AuthMe getInstanceOfAuthMe();

    public Module getInstance();

    public enum ModuleType {
        MANAGER,
        MYSQL,
        REDIS,
        ACTIONS,
        CONVERTERS,
        EMAILS,
        CUSTOM;
    }

    public ModuleType getType();

    public boolean load();

    public boolean unload();
}
