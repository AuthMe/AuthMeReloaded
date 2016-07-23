package fr.xephi.authme.datasource;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.DatabaseSettings;

/**
 * Database column names.
 */
public final class Columns {

    public final String NAME;
    public final String REAL_NAME;
    public final String PASSWORD;
    public final String SALT;
    public final String IP;
    public final String LAST_LOGIN;
    public final String GROUP;
    public final String LASTLOC_X;
    public final String LASTLOC_Y;
    public final String LASTLOC_Z;
    public final String LASTLOC_WORLD;
    public final String EMAIL;
    public final String ID;
    public final String IS_LOGGED;

    public Columns(Settings settings) {
        NAME          = settings.getProperty(DatabaseSettings.MYSQL_COL_NAME);
        REAL_NAME     = settings.getProperty(DatabaseSettings.MYSQL_COL_REALNAME);
        PASSWORD      = settings.getProperty(DatabaseSettings.MYSQL_COL_PASSWORD);
        SALT          = settings.getProperty(DatabaseSettings.MYSQL_COL_SALT);
        IP            = settings.getProperty(DatabaseSettings.MYSQL_COL_IP);
        LAST_LOGIN    = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOGIN);
        GROUP         = settings.getProperty(DatabaseSettings.MYSQL_COL_GROUP);
        LASTLOC_X     = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_X);
        LASTLOC_Y     = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_Y);
        LASTLOC_Z     = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_Z);
        LASTLOC_WORLD = settings.getProperty(DatabaseSettings.MYSQL_COL_LASTLOC_WORLD);
        EMAIL         = settings.getProperty(DatabaseSettings.MYSQL_COL_EMAIL);
        ID            = settings.getProperty(DatabaseSettings.MYSQL_COL_ID);
        IS_LOGGED     = settings.getProperty(DatabaseSettings.MYSQL_COL_ISLOGGED);
    }

}
