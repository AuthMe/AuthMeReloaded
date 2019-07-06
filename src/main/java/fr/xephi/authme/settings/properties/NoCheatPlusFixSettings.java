package fr.xephi.authme.settings.properties;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

public class NoCheatPlusFixSettings implements SettingsHolder {

    @Comment("Set this to true if players get kicked for flying when they try to login")
    public static final Property<Boolean> ENABLE_FIX = newProperty("NoCheatPlus.fixKickForFlying", false);

    private NoCheatPlusFixSettings() {
    }
}
