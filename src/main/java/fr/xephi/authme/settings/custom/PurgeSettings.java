package fr.xephi.authme.settings.custom;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fr.xephi.authme.settings.custom.annotations.Comment;
import fr.xephi.authme.settings.custom.annotations.Type;
import fr.xephi.authme.settings.custom.annotations.Type.SettingType;

public class PurgeSettings extends CustomSetting {

	@Comment("If enabled, AuthMe automatically purges old, unused accounts")
	@Type(SettingType.Boolean)
	public boolean useAutoPurge = false;

	@Comment("Number of Days an account become Unused")
	@Type(SettingType.Int)
	public int daysBeforeRemovePlayer = 60;

	@Comment("Do we need to remove the player.dat file during purge process?")
	@Type(SettingType.Boolean)
	public boolean removePlayerDat = false;

	@Comment("Do we need to remove the Essentials/users/player.yml file during purge process?")
	@Type(SettingType.Boolean)
	public boolean removeEssentialsFiles = false;

	@Comment("World where are players.dat stores")
	@Type(SettingType.String)
	public String defaultWorld = "world";

	@Comment("Do we need to remove LimitedCreative/inventories/player.yml, player_creative.yml files during purge process ?")
	@Type(SettingType.Boolean)
	public boolean removeLimiteCreativeInventories = false;

	@Comment("Do we need to remove the AntiXRayData/PlayerData/player file during purge process?")
	@Type(SettingType.Boolean)
	public boolean removeAntiXRayFile = false;

	@Comment("Do we need to remove permissions?")
	@Type(SettingType.Boolean)
	public boolean removePermissions = false;

	private static File configFile = new File("." + File.separator + "plugins" + File.separator + "AuthMe" + File.separator + "purge.yml");

	private PurgeSettings instance;

	public PurgeSettings()
	{
		super(configFile);
		instance = this;
	}

	public PurgeSettings getInstance() {
		return instance;
	}

	public void setInstance(PurgeSettings instance) {
		this.instance = instance;
	}
}
