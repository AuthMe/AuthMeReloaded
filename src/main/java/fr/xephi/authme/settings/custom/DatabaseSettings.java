package fr.xephi.authme.settings.custom;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;

import static fr.xephi.authme.settings.domain.Property.newProperty;
import static fr.xephi.authme.settings.domain.PropertyType.BOOLEAN;
import static fr.xephi.authme.settings.domain.PropertyType.STRING;

public class DatabaseSettings implements SettingsClass {

    @Comment({"What type of database do you want to use?",
            "Valid values: sqlite, mysql"})
    public static final Property<DataSource.DataSourceType> BACKEND =
        newProperty(DataSource.DataSourceType.class, "DataSource.backend", DataSource.DataSourceType.SQLITE);

    @Comment("Enable database caching, should improve database performance")
    public static final Property<Boolean> USE_CACHING =
        newProperty(BOOLEAN, "DataSource.caching", true);

    @Comment("Database host address")
    public static final Property<String> MYSQL_HOST =
        newProperty(STRING, "DataSource.mySQLHost", "127.0.0.1");

    @Comment("Database port")
    public static final Property<String> MYSQL_PORT =
        newProperty(STRING, "DataSource.mySQLPort", "3306");

    @Comment("Username about Database Connection Infos")
    public static final Property<String> MYSQL_USERNAME =
        newProperty(STRING, "DataSource.mySQLUsername", "authme");

    @Comment("Password about Database Connection Infos")
    public static final Property<String> MYSQL_PASSWORD =
        newProperty(STRING, "DataSource.mySQLPassword", "123456");

    @Comment("Database Name, use with converters or as SQLITE database name")
    public static final Property<String> MYSQL_DATABASE =
        newProperty(STRING, "DataSource.mySQLDatabase", "authme");

    @Comment("Table of the database")
    public static final Property<String> MYSQL_TABLE =
        newProperty(STRING, "DataSource.mySQLTablename", "authme");

    @Comment("Column of IDs to sort data")
    public static final Property<String> MYSQL_COL_ID =
        newProperty(STRING, "DataSource.mySQLColumnId", "id");

    @Comment("Column for storing or checking players nickname")
    public static final Property<String> MYSQL_COL_NAME =
        newProperty(STRING, "DataSource.mySQLColumnName", "username");

    @Comment("Column for storing or checking players RealName ")
    public static final Property<String> MYSQL_COL_REALNAME =
        newProperty(STRING, "DataSource.mySQLRealName", "realname");

    @Comment("Column for storing players passwords")
    public static final Property<String> MYSQL_COL_PASSWORD =
        newProperty(STRING, "DataSource.mySQLColumnPassword", "password");

    @Comment("Column for storing players passwords salts")
    public static final Property<String> MYSQL_COL_SALT =
        newProperty(STRING, "ExternalBoardOptions.mySQLColumnSalt", "");

    @Comment("Column for storing players emails")
    public static final Property<String> MYSQL_COL_EMAIL =
        newProperty(STRING, "DataSource.mySQLColumnEmail", "email");

    @Comment("Column for storing if a player is logged in or not")
    public static final Property<String> MYSQL_COL_ISLOGGED =
        newProperty(STRING, "DataSource.mySQLColumnLogged", "isLogged");

    @Comment("Column for storing players ips")
    public static final Property<String> MYSQL_COL_IP =
        newProperty(STRING, "DataSource.mySQLColumnIp", "ip");

    @Comment("Column for storing players lastlogins")
    public static final Property<String> MYSQL_COL_LASTLOGIN =
        newProperty(STRING, "DataSource.mySQLColumnLastLogin", "lastlogin");

    @Comment("Column for storing player LastLocation - X")
    public static final Property<String> MYSQL_COL_LASTLOC_X =
        newProperty(STRING, "DataSource.mySQLlastlocX", "x");

    @Comment("Column for storing player LastLocation - Y")
    public static final Property<String> MYSQL_COL_LASTLOC_Y =
        newProperty(STRING, "DataSource.mySQLlastlocY", "y");

    @Comment("Column for storing player LastLocation - Z")
    public static final Property<String> MYSQL_COL_LASTLOC_Z =
        newProperty(STRING, "DataSource.mySQLlastlocZ", "z");

    @Comment("Column for storing player LastLocation - World Name")
    public static final Property<String> MYSQL_COL_LASTLOC_WORLD =
        newProperty(STRING, "DataSource.mySQLlastlocWorld", "world");

    @Comment("Column for storing players groups")
    public static final Property<String> MYSQL_COL_GROUP =
        newProperty(STRING, "ExternalBoardOptions.mySQLColumnGroup", "");

    @Comment("Enable this when you allow registration through a website")
    public static final Property<Boolean> MYSQL_WEBSITE =
        newProperty(BOOLEAN, "DataSource.mySQLWebsite", false);

    private DatabaseSettings() {
    }

}
