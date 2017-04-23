package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public final class BackupSettings implements SettingsHolder {

    @Comment("General configuration for backups: if false, no backups are possible")
    public static final Property<Boolean> ENABLED =
        newProperty("BackupSystem.ActivateBackup", false);

    @Comment("Create backup at every start of server")
    public static final Property<Boolean> ON_SERVER_START =
        newProperty("BackupSystem.OnServerStart", false);

    @Comment("Create backup at every stop of server")
    public static final Property<Boolean> ON_SERVER_STOP =
        newProperty("BackupSystem.OnServerStop", true);

    @Comment("Windows only: MySQL installation path")
    public static final Property<String> MYSQL_WINDOWS_PATH =
        newProperty("BackupSystem.MysqlWindowsPath", "C:\\Program Files\\MySQL\\MySQL Server 5.1\\");

    private BackupSettings() {
    }
}
