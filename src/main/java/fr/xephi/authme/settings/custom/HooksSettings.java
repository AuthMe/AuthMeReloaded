package fr.xephi.authme.settings.custom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.xephi.authme.settings.custom.annotations.Comment;
import fr.xephi.authme.settings.custom.annotations.Type;
import fr.xephi.authme.settings.custom.annotations.Type.SettingType;

public class HooksSettings extends CustomSetting {

	@Comment("Do we need to hook with multiverse for spawn checking?")
	@Type(SettingType.Boolean)
	public boolean multiverse = true;

	@Comment("Do we need to hook with BungeeCord ?")
	@Type(SettingType.Boolean)
	public boolean bungeecord = false;

	@Comment("Send player to this BungeeCord server after register/login")
	@Type(SettingType.String)
	public String sendPlayerTo = "";

	@Comment("Do we need to disable Essentials SocialSpy on join?")
	@Type(SettingType.Boolean)
	public boolean disableSocialSpy = false;

	@Comment("Do we need to force /motd Essentials command on join?")
	@Type(SettingType.Boolean)
	public boolean useEssentialsMotd = false;

	@Comment("Do we need to cache custom Attributes?")
	@Type(SettingType.Boolean)
	public boolean customAttributes = false;

	private static File configFile = new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "hooks.yml");

	private HooksSettings instance;

	public HooksSettings()
	{
		super(configFile);
		instance = this;
	}

	public HooksSettings getInstance() {
		return instance;
	}

	public void setInstance(HooksSettings instance) {
		this.instance = instance;
	}
}
