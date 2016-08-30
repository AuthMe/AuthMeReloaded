package fr.xephi.authme.settings.properties;

import com.github.authme.configme.Comment;
import com.github.authme.configme.SettingsHolder;
import com.github.authme.configme.properties.Property;

import static com.github.authme.configme.properties.PropertyInitializer.newProperty;

public class ConverterSettings implements SettingsHolder {

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
