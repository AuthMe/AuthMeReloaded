package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;

import static fr.xephi.authme.settings.domain.Property.newProperty;

public class BackupSettings implements SettingsClass {

    @Comment("Enable or disable automatic backup")
    public static final Property<Boolean> ENABLED =
        newProperty("BackupSystem.ActivateBackup", false);

    @Comment("Set backup at every start of server")
    public static final Property<Boolean> ON_SERVER_START =
        newProperty("BackupSystem.OnServerStart", false);

    @Comment("Set backup at every stop of server")
    public static final Property<Boolean> ON_SERVER_STOP =
        newProperty("BackupSystem.OnServerStop", true);

    @Comment(" Windows only mysql installation Path")
    public static final Property<String> MYSQL_WINDOWS_PATH =
        newProperty("BackupSystem.MysqlWindowsPath", "C:\\Program Files\\MySQL\\MySQL Server 5.1\\");

    private BackupSettings() {
    }
}
