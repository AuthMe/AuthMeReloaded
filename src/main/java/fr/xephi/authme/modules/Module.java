package fr.xephi.authme.modules;

public abstract class Module {

    enum ModuleType {
        MANAGER,
        MYSQL,
        REDIS,
        ACTIONS,
        CONVERTERS,
        EMAILS,
        CUSTOM
    }

    public abstract String getName();

    public abstract ModuleType getType();

    public void load() {
    }

    public void unload() {
    }
}
