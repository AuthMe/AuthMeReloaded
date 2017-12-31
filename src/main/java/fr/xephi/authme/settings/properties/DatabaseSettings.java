package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;
import fr.xephi.authme.datasource.DataSourceType;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public final class DatabaseSettings implements SettingsHolder {

    @Comment({"What type of database do you want to use?",
        "Valid values: SQLITE, MYSQL, POSTGRESQL"})
    public static final Property<DataSourceType> BACKEND =
        newProperty(DataSourceType.class, "DataSource.backend", DataSourceType.SQLITE);

    @Comment({"Enable the database caching system, should be disabled on bungeecord environments",
        "or when a website integration is being used."})
    public static final Property<Boolean> USE_CACHING =
        newProperty("DataSource.caching", true);

    @Comment("Database host address")
    public static final Property<String> MYSQL_HOST =
        newProperty("DataSource.mySQLHost", "127.0.0.1");

    @Comment("Database port")
    public static final Property<String> MYSQL_PORT =
        newProperty("DataSource.mySQLPort", "3306");

    @Comment("Connect to MySQL database over SSL")
    public static final Property<Boolean> MYSQL_USE_SSL =
        newProperty("DataSource.mySQLUseSSL", true);

    @Comment("Username to connect to the MySQL database")
    public static final Property<String> MYSQL_USERNAME =
        newProperty("DataSource.mySQLUsername", "authme");

    @Comment("Password to connect to the MySQL database")
    public static final Property<String> MYSQL_PASSWORD =
        newProperty("DataSource.mySQLPassword", "12345");

    @Comment("Database Name, use with converters or as SQLITE database name")
    public static final Property<String> MYSQL_DATABASE =
        newProperty("DataSource.mySQLDatabase", "authme");

    @Comment("Table of the database")
    public static final Property<String> MYSQL_TABLE =
        newProperty("DataSource.mySQLTablename", "authme");

    @Comment("Column of IDs to sort data")
    public static final Property<String> MYSQL_COL_ID =
        newProperty("DataSource.mySQLColumnId", "id");

    @Comment("Column for storing or checking players nickname")
    public static final Property<String> MYSQL_COL_NAME =
        newProperty("DataSource.mySQLColumnName", "username");

    @Comment("Column for storing or checking players RealName")
    public static final Property<String> MYSQL_COL_REALNAME =
        newProperty("DataSource.mySQLRealName", "realname");

    @Comment("Column for storing players passwords")
    public static final Property<String> MYSQL_COL_PASSWORD =
        newProperty("DataSource.mySQLColumnPassword", "password");

    @Comment("Column for storing players passwords salts")
    public static final Property<String> MYSQL_COL_SALT =
        newProperty("ExternalBoardOptions.mySQLColumnSalt", "");

    @Comment("Column for storing players emails")
    public static final Property<String> MYSQL_COL_EMAIL =
        newProperty("DataSource.mySQLColumnEmail", "email");

    @Comment("Column for storing if a player is logged in or not")
    public static final Property<String> MYSQL_COL_ISLOGGED =
        newProperty("DataSource.mySQLColumnLogged", "isLogged");

    @Comment("Column for storing if a player has a valid session or not")
    public static final Property<String> MYSQL_COL_HASSESSION =
        newProperty("DataSource.mySQLColumnHasSession", "hasSession");

    @Comment("Column for storing the player's last IP")
    public static final Property<String> MYSQL_COL_LAST_IP =
        newProperty("DataSource.mySQLColumnIp", "ip");

    @Comment("Column for storing players lastlogins")
    public static final Property<String> MYSQL_COL_LASTLOGIN =
        newProperty("DataSource.mySQLColumnLastLogin", "lastlogin");

    @Comment("Column storing the registration date")
    public static final Property<String> MYSQL_COL_REGISTER_DATE =
        newProperty("DataSource.mySQLColumnRegisterDate", "regdate");

    @Comment("Column for storing the IP address at the time of registration")
    public static final Property<String> MYSQL_COL_REGISTER_IP =
        newProperty("DataSource.mySQLColumnRegisterIp", "regip");

    @Comment("Column for storing player LastLocation - X")
    public static final Property<String> MYSQL_COL_LASTLOC_X =
        newProperty("DataSource.mySQLlastlocX", "x");

    @Comment("Column for storing player LastLocation - Y")
    public static final Property<String> MYSQL_COL_LASTLOC_Y =
        newProperty("DataSource.mySQLlastlocY", "y");

    @Comment("Column for storing player LastLocation - Z")
    public static final Property<String> MYSQL_COL_LASTLOC_Z =
        newProperty("DataSource.mySQLlastlocZ", "z");

    @Comment("Column for storing player LastLocation - World Name")
    public static final Property<String> MYSQL_COL_LASTLOC_WORLD =
        newProperty("DataSource.mySQLlastlocWorld", "world");

    @Comment("Column for storing player LastLocation - Yaw")
    public static final Property<String> MYSQL_COL_LASTLOC_YAW =
        newProperty("DataSource.mySQLlastlocYaw", "yaw");

    @Comment("Column for storing player LastLocation - Pitch")
    public static final Property<String> MYSQL_COL_LASTLOC_PITCH =
        newProperty("DataSource.mySQLlastlocPitch", "pitch");

    @Comment("Column for storing players groups")
    public static final Property<String> MYSQL_COL_GROUP =
        newProperty("ExternalBoardOptions.mySQLColumnGroup", "");

    @Comment("Overrides the size of the DB Connection Pool, default = 10")
    public static final Property<Integer> MYSQL_POOL_SIZE =
        newProperty("DataSource.poolSize", 10);

    @Comment({"The maximum lifetime of a connection in the pool, default = 1800 seconds",
        "You should set this at least 30 seconds less than mysql server wait_timeout"})
    public static final Property<Integer> MYSQL_CONNECTION_MAX_LIFETIME =
        newProperty("DataSource.maxLifetime", 1800);

    private DatabaseSettings() {
    }

}
