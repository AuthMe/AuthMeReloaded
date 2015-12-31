package fr.xephi.authme.settings.custom;

import java.io.File;

import fr.xephi.authme.settings.custom.annotations.Comment;
import fr.xephi.authme.settings.custom.annotations.Type;
import fr.xephi.authme.settings.custom.annotations.Type.SettingType;

public class DatabaseSettings extends CustomSetting {

	@Comment({"What type of database do you want to use?",
			"Valid values: sqlite, mysql"})
	@Type(SettingType.String)
	public String backend = "sqlite";

	@Comment("Enable database caching, should improve database performance")
	@Type(SettingType.Boolean)
	public boolean caching = true;

	@Comment("Database host address")
	@Type(SettingType.String)
	public String mySQLHost = "127.0.0.1";

	@Comment("Database port")
	@Type(SettingType.String)
	public String mySQLPort = "3306";

	@Comment("Username about Database Connection Infos")
	@Type(SettingType.String)
	public String mySQLUsername = "authme";

	@Comment("Password about Database Connection Infos")
	@Type(SettingType.String)
	public String mySQLPassword = "12345";

	@Comment("Database Name, use with converters or as SQLITE database name")
	@Type(SettingType.String)
	public String mySQLDatabase = "authme";

	@Comment("Table of the database")
	@Type(SettingType.String)
	public String mySQLTablename = "authme";

	@Comment("Column of IDs to sort data")
	@Type(SettingType.String)
	public String mySQLColumnId = "id";

	@Comment("Column for storing or checking players nickname")
	@Type(SettingType.String)
	public String mySQLColumnName = "username";

	@Comment("Column for storing or checking players RealName ")
	@Type(SettingType.String)
	public String mySQLColumnRealName = "realname";

	@Comment("Column for storing players passwords")
	@Type(SettingType.String)
	public String mySQLColumnPassword = "password";

	@Comment("Column for storing players passwords salts")
	@Type(SettingType.String)
	public String mySQLColumnSalt = "";

	@Comment("Column for storing players emails")
	@Type(SettingType.String)
	public String mySQLColumnEmail = "email";

	@Comment("Column for storing if a player is logged in or not")
	@Type(SettingType.String)
	public String mySQLColumnLogged = "isLogged";

	@Comment("Column for storing players ips")
	@Type(SettingType.String)
	public String mySQLColumnIp = "ip";

	@Comment("Column for storing players lastlogins")
	@Type(SettingType.String)
	public String mySQLColumnLastLogin = "lastlogin";

	@Comment("Column for storing player LastLocation - X")
	@Type(SettingType.String)
	public String mySQLColumnLastLocX = "x";

	@Comment("Column for storing player LastLocation - Y")
	@Type(SettingType.String)
	public String mySQLColumnLastLocY = "y";

	@Comment("Column for storing player LastLocation - Z")
	@Type(SettingType.String)
	public String mySQLColumnLastLocZ = "z";

	@Comment("Column for storing player LastLocation - World Name")
	@Type(SettingType.String)
	public String mySQLColumnLastLocWorld = "world";

	@Comment("Column for storing players groups")
	@Type(SettingType.String)
	public String mySQLColumnGroup = "";

	@Comment("Enable this when you allow registration through a website")
	@Type(SettingType.Boolean)
	public boolean mySQLWebsite = false;

	private static File configFile = new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "database.yml");

	private DatabaseSettings instance;

	public DatabaseSettings()
	{
		super(configFile);
		instance = this;
	}

	public DatabaseSettings getInstance() {
		return instance;
	}

	public void setInstance(DatabaseSettings instance) {
		this.instance = instance;
	}
}
