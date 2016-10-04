package fr.xephi.authme.settings.properties;

import com.github.authme.configme.Comment;
import com.github.authme.configme.SettingsHolder;
import com.github.authme.configme.properties.Property;

import static com.github.authme.configme.properties.PropertyInitializer.newProperty;

public class PurgeSettings implements SettingsHolder {

    @Comment("If enabled, AuthMe automatically purges old, unused accounts")
    public static final Property<Boolean> USE_AUTO_PURGE =
        newProperty("Purge.useAutoPurge", false);

    @Comment("Number of Days an account become Unused")
    public static final Property<Integer> DAYS_BEFORE_REMOVE_PLAYER =
        newProperty("Purge.daysBeforeRemovePlayer", 60);

    @Comment("Do we need to remove the player.dat file during purge process?")
    public static final Property<Boolean> REMOVE_PLAYER_DAT =
        newProperty("Purge.removePlayerDat", false);

    @Comment("Do we need to remove the Essentials/userdata/player.yml file during purge process?")
    public static final Property<Boolean> REMOVE_ESSENTIALS_FILES =
        newProperty("Purge.removeEssentialsFile", false);

    @Comment("World where are players.dat stores")
    public static final Property<String> DEFAULT_WORLD =
        newProperty("Purge.defaultWorld", "world");

    @Comment("Do we need to remove LimitedCreative/inventories/player.yml, player_creative.yml files during purge process ?")
    public static final Property<Boolean> REMOVE_LIMITED_CREATIVE_INVENTORIES =
        newProperty("Purge.removeLimitedCreativesInventories", false);

    @Comment("Do we need to remove the AntiXRayData/PlayerData/player file during purge process?")
    public static final Property<Boolean> REMOVE_ANTI_XRAY_FILE =
        newProperty("Purge.removeAntiXRayFile", false);

    @Comment("Do we need to remove permissions?")
    public static final Property<Boolean> REMOVE_PERMISSIONS =
        newProperty("Purge.removePermissions", false);

    private PurgeSettings() {
    }

}
