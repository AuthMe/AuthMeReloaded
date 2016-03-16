package fr.xephi.authme.settings.properties;

import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;

import static fr.xephi.authme.settings.domain.Property.newProperty;

public class ConverterSettings implements SettingsClass {

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
