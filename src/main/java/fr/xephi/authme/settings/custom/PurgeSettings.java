package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;

import static fr.xephi.authme.settings.domain.Property.newProperty;
import static fr.xephi.authme.settings.domain.PropertyType.BOOLEAN;
import static fr.xephi.authme.settings.domain.PropertyType.INTEGER;
import static fr.xephi.authme.settings.domain.PropertyType.STRING;

public class PurgeSettings implements SettingsClass {

    @Comment("If enabled, AuthMe automatically purges old, unused accounts")
    public static final Property<Boolean> USE_AUTO_PURGE =
        newProperty(BOOLEAN, "Purge.useAutoPurge", false);

    @Comment("Number of Days an account become Unused")
    public static final Property<Integer> DAYS_BEFORE_REMOVE_PLAYER =
        newProperty(INTEGER, "Purge.daysBeforeRemovePlayer", 60);

    @Comment("Do we need to remove the player.dat file during purge process?")
    public static final Property<Boolean> REMOVE_PLAYER_DAT =
        newProperty(BOOLEAN, "Purge.removePlayerDat", false);

    @Comment("Do we need to remove the Essentials/users/player.yml file during purge process?")
    public static final Property<Boolean> REMOVE_ESSENTIALS_FILES =
        newProperty(BOOLEAN, "Purge.removeEssentialsFile", false);

    @Comment("World where are players.dat stores")
    public static final Property<String> DEFAULT_WORLD =
        newProperty(STRING, "Purge.defaultWorld", "world");

    @Comment("Do we need to remove LimitedCreative/inventories/player.yml, player_creative.yml files during purge process ?")
    public static final Property<Boolean> REMOVE_LIMITED_CREATIVE_INVENTORIES =
        newProperty(BOOLEAN, "Purge.removeLimitedCreativesInventories", false);

    @Comment("Do we need to remove the AntiXRayData/PlayerData/player file during purge process?")
    public static final Property<Boolean> REMOVE_ANTI_XRAY_FILE =
        newProperty(BOOLEAN, "Purge.removeAntiXRayFile", false);

    @Comment("Do we need to remove permissions?")
    public static final Property<Boolean> REMOVE_PERMISSIONS =
        newProperty(BOOLEAN, "Purge.removePermissions", false);

    private PurgeSettings() {
    }

}
