package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public final class ConverterSettings implements SettingsHolder {

    @Comment("Rakamak file name")
    public static final Property<String> RAKAMAK_FILE_NAME =
        newProperty("Converter.Rakamak.fileName", "users.rak");

    @Comment("Rakamak use IP?")
    public static final Property<Boolean> RAKAMAK_USE_IP =
        newProperty("Converter.Rakamak.useIP", false);

    @Comment("Rakamak IP file name")
    public static final Property<String> RAKAMAK_IP_FILE_NAME =
        newProperty("Converter.Rakamak.ipFileName", "UsersIp.rak");

    @Comment("CrazyLogin database file name")
    public static final Property<String> CRAZYLOGIN_FILE_NAME =
        newProperty("Converter.CrazyLogin.fileName", "accounts.db");

    private ConverterSettings() {
    }

}
