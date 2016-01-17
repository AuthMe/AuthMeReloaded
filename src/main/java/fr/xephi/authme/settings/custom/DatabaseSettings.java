package fr.xephi.authme.settings.custom;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;

import static fr.xephi.authme.settings.domain.Property.newProperty;

public class DatabaseSettings implements SettingsClass {

    @Comment({"What type of database do you want to use?",
            "Valid values: sqlite, mysql"})
    public static final Property<DataSource.DataSourceType> BACKEND =
        newProperty(DataSource.DataSourceType.class, "DataSource.backend", DataSource.DataSourceType.SQLITE);

    @Comment("Enable database caching, should improve database performance")
    public static final Property<Boolean> USE_CACHING =
        newProperty("DataSource.caching", true);

    @Comment("Database host address")
    public static final Property<String> MYSQL_HOST =
        newProperty("DataSource.mySQLHost", "127.0.0.1");

    @Comment("Database port")
    public static final Property<String> MYSQL_PORT =
        newProperty("DataSource.mySQLPort", "3306");

    @Comment("Username about Database Connection Infos")
    public static final Property<String> MYSQL_USERNAME =
        newProperty("DataSource.mySQLUsername", "authme");

    @Comment("Password about Database Connection Infos")
    public static final Property<String> MYSQL_PASSWORD =
        newProperty("DataSource.mySQLPassword", "123456");

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

    @Comment("Column for storing or checking players RealName ")
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

    @Comment("Column for storing players ips")
    public static final Property<String> MYSQL_COL_IP =
        newProperty("DataSource.mySQLColumnIp", "ip");

    @Comment("Column for storing players lastlogins")
    public static final Property<String> MYSQL_COL_LASTLOGIN =
        newProperty("DataSource.mySQLColumnLastLogin", "lastlogin");

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

    @Comment("Column for storing players groups")
    public static final Property<String> MYSQL_COL_GROUP =
        newProperty("ExternalBoardOptions.mySQLColumnGroup", "");

    @Comment("Enable this when you allow registration through a website")
    public static final Property<Boolean> MYSQL_WEBSITE =
        newProperty("DataSource.mySQLWebsite", false);

    private DatabaseSettings() {
    }

}
