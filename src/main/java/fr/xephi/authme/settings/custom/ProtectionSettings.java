package fr.xephi.authme.settings.custom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.xephi.authme.settings.custom.annotations.Comment;
import fr.xephi.authme.settings.custom.annotations.Type;
import fr.xephi.authme.settings.custom.annotations.Type.SettingType;

public class ProtectionSettings extends CustomSetting {

	@Comment("Enable some servers protection ( country based login, antibot )")
	@Type(SettingType.Boolean)
	public boolean enableProtection = false;

	@Comment({"Countries allowed to join the server and register, see http://dev.bukkit.org/bukkit-plugins/authme-reloaded/pages/countries-codes/ for countries' codes",
			"PLEASE USE QUOTES !"})
	@Type(SettingType.StringList)
	public List<String> countriesWhitelist = new ArrayList<String>();

	@Comment({"Countries not allowed to join the server and register",
	"PLEASE USE QUOTES !"})
	@Type(SettingType.StringList)
	public List<String> countriesBlacklist = new ArrayList<String>();

	@Comment("Do we need to enable automatic antibot system?")
	@Type(SettingType.Boolean)
	public boolean enableAntiBot = false;

	@Comment("Max number of player allowed to login in 5 secs before enable AntiBot system automatically")
	@Type(SettingType.Int)
	public int antiBotSensibility = 5;

	@Comment("Duration in minutes of the antibot automatic system")
	@Type(SettingType.Int)
	public int antiBotDuration = 10;

	private static File configFile = new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "protection.yml");

	private ProtectionSettings instance;

	public ProtectionSettings()
	{
		super(configFile);
		instance = this;
		if (this.isFirstLaunch)
		{
			this.countriesWhitelist.add("US");
			this.countriesWhitelist.add("GB");
			this.countriesBlacklist.add("A1");
			save();
		}
	}

	public ProtectionSettings getInstance() {
		return instance;
	}

	public void setInstance(ProtectionSettings instance) {
		this.instance = instance;
	}
}
