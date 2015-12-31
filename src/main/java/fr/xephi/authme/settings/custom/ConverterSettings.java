package fr.xephi.authme.settings.custom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.xephi.authme.settings.custom.annotations.Comment;
import fr.xephi.authme.settings.custom.annotations.Type;
import fr.xephi.authme.settings.custom.annotations.Type.SettingType;

public class ConverterSettings extends CustomSetting {

	@Comment("Rakamak file name")
	@Type(SettingType.String)
	public String rakamakFileName = "users.rak";

	@Comment("Rakamak use Ip ?")
	@Type(SettingType.Boolean)
	public boolean rakamakeUseIP = false;

	@Comment("Rakamak IP file name")
	@Type(SettingType.String)
	public String rakamakIPFileName = "UsersIp.rak";

	@Comment("CrazyLogin database file name")
	@Type(SettingType.String)
	public String crazyLoginFileName = "accounts.db";

	private static File configFile = new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "converter.yml");

	private ConverterSettings instance;

	public ConverterSettings()
	{
		super(configFile);
		instance = this;
	}

	public ConverterSettings getInstance() {
		return instance;
	}

	public void setInstance(ConverterSettings instance) {
		this.instance = instance;
	}
}
