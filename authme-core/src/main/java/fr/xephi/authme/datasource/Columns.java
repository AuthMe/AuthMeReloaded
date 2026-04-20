package fr.xephi.authme.datasource;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

/**
 * Database column names.
 */
// Justification: String is immutable and this class is used to easily access the configurable column names
@SuppressWarnings({"checkstyle:VisibilityModifier", "checkstyle:MemberName", "checkstyle:AbbreviationAsWordInName"})
public final class Columns {

    public final String NAME;
    public final String REAL_NAME;
    public final String PASSWORD;
    public final String SALT;
    public final String TOTP_KEY;
    public final String LAST_IP;
    public final String LAST_LOGIN;
    public final String GROUP;
    public final String LASTLOC_X;
    public final String LASTLOC_Y;
    public final String LASTLOC_Z;
    public final String LASTLOC_WORLD;
    public final String LASTLOC_YAW;
    public final String LASTLOC_PITCH;
    public final String EMAIL;
    public final String ID;
    public final String IS_LOGGED;
    public final String HAS_SESSION;
    public final String REGISTRATION_DATE;
    public final String REGISTRATION_IP;
    public final String PLAYER_UUID;

    public Columns(Settings settings) {
        NAME              = settings.getProperty(DatabaseSettings.MYSQL_COL_NAME);
        REAL_NAME         = settings.getProperty(DatabaseSettings.MYSQL_COL_REALNAME);
        PASSWORD          = settings.getProperty(DatabaseSettings.MYSQL_COL_PASSWORD);
        SALT              = settings.getProperty(DatabaseSettings.MYSQL_COL_SALT);
        TOTP_KEY          = settings.getProperty(DatabaseSettings.MYSQL_COL_TOTP_KEY);
        LAST_IP           = settings.getProperty(DatabaseSettings.MYSQL_COL_LAST_IP);
        LAST_LOGIN        = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOGIN);
        GROUP             = settings.getProperty(DatabaseSettings.MYSQL_COL_GROUP);
        LASTLOC_X         = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_X);
        LASTLOC_Y         = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_Y);
        LASTLOC_Z         = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_Z);
        LASTLOC_WORLD     = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_WORLD);
        LASTLOC_YAW       = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_YAW);
        LASTLOC_PITCH     = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_PITCH);
        EMAIL             = settings.getProperty(DatabaseSettings.MYSQL_COL_EMAIL);
        ID                = settings.getProperty(DatabaseSettings.MYSQL_COL_ID);
        IS_LOGGED         = settings.getProperty(DatabaseSettings.MYSQL_COL_ISLOGGED);
        HAS_SESSION       = settings.getProperty(DatabaseSettings.MYSQL_COL_HASSESSION);
        REGISTRATION_DATE = settings.getProperty(DatabaseSettings.MYSQL_COL_REGISTER_DATE);
        REGISTRATION_IP   = settings.getProperty(DatabaseSettings.MYSQL_COL_REGISTER_IP);
        PLAYER_UUID       = settings.getProperty(DatabaseSettings.MYSQL_COL_PLAYER_UUID);
    }

}
