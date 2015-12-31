package fr.xephi.authme.settings.custom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.xephi.authme.settings.custom.annotations.Comment;
import fr.xephi.authme.settings.custom.annotations.Type;
import fr.xephi.authme.settings.custom.annotations.Type.SettingType;

public class SecuritySettings extends CustomSetting {

	@Comment({"Stop the server if we can't contact the sql database",
        "Take care with this, if you set that to false,",
        "AuthMe automatically disable and the server is not protected!"})
	@Type(SettingType.Boolean)
	public boolean stopServerOnProblem = true;

	@Comment("/reload support")
	@Type(SettingType.Boolean)
	public boolean useReloadCommandSupport = true;

	@Comment("Remove Spam from Console ?")
	@Type(SettingType.Boolean)
	public boolean removeSpamFromConsole = false;

	@Comment("Remove Password from Console ?")
	@Type(SettingType.Boolean)
	public boolean removePasswordFromConsole = true;

	@Comment("Player need to put a captcha when he fails too lot the password")
	@Type(SettingType.Boolean)
	public boolean useCaptcha = false;

	@Comment("Max allowed tries before request a captcha")
	@Type(SettingType.Int)
	public int maxLoginTryBeforeCaptcha = 5;

	@Comment("Captcha length ")
	@Type(SettingType.Int)
	public int captchaLength = 5;

	private static File configFile = new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "security.yml");

	private SecuritySettings instance;

	public SecuritySettings()
	{
		super(configFile);
		instance = this;
	}

	public SecuritySettings getInstance() {
		return instance;
	}

	public void setInstance(SecuritySettings instance) {
		this.instance = instance;
	}
}
