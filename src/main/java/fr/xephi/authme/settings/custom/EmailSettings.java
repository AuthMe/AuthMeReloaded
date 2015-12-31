package fr.xephi.authme.settings.custom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.xephi.authme.settings.custom.annotations.Comment;
import fr.xephi.authme.settings.custom.annotations.Type;
import fr.xephi.authme.settings.custom.annotations.Type.SettingType;

public class EmailSettings extends CustomSetting {

	@Comment("Email SMTP server host")
	@Type(SettingType.String)
	public String mailSMTP = "smtp.gmail.com";

	@Comment("Email SMTP server port")
	@Type(SettingType.Int)
	public int mailPort = 465;

	@Comment("Email account whose send the mail")
	@Type(SettingType.String)
	public String mailAccount = "";

	@Comment("Email account password")
	@Type(SettingType.String)
	public String mailPassword = "";

	@Comment("Random password length")
	@Type(SettingType.Int)
	public int recoveryPasswordLength = 8;

	@Comment("Mail Subject")
	@Type(SettingType.String)
	public String mailSubject = "Your new AuthMe password";

	@Comment("Like maxRegPerIP but with email")
	@Type(SettingType.Int)
	public int maxRegPerEmail = 1;

	@Comment("Recall players to add an email ?")
	@Type(SettingType.Boolean)
	public boolean recallPlayers = false;

	@Comment("Delay in minute for the recall scheduler")
	@Type(SettingType.Int)
	public int delayRecall = 5;

	@Comment("Blacklist these domains for emails")
	@Type(SettingType.StringList)
	public List<String> emailBlackListed = new ArrayList<String>();

	@Comment("Whitelist ONLY these domains for emails")
	@Type(SettingType.StringList)
	public List<String> emailWhiteListed = new ArrayList<String>();

	@Comment("Do we need to send new password draw in an image ?")
	@Type(SettingType.Boolean)
	public boolean generateImage = false;

	private static File configFile = new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "emails.yml");

	private EmailSettings instance;

	public EmailSettings()
	{
		super(configFile);
		instance = this;
		if (this.isFirstLaunch)
		{
			this.emailBlackListed.add("10minutemail.com");
			save();
		}
	}

	public EmailSettings getInstance() {
		return instance;
	}

	public void setInstance(EmailSettings instance) {
		this.instance = instance;
	}
}
