package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public final class PurgeSettings implements SettingsHolder {

    @Comment("If enabled, AuthMe automatically purges old, unused accounts")
    public static final Property<Boolean> USE_AUTO_PURGE =
        newProperty("Purge.useAutoPurge", false);

    @Comment("Number of days after which an account should be purged")
    public static final Property<Integer> DAYS_BEFORE_REMOVE_PLAYER =
        newProperty("Purge.daysBeforeRemovePlayer", 60);

    @Comment("Do we need to remove the player.dat file during purge process?")
    public static final Property<Boolean> REMOVE_PLAYER_DAT =
        newProperty("Purge.removePlayerDat", false);

    @Comment("Do we need to remove the Essentials/userdata/player.yml file during purge process?")
    public static final Property<Boolean> REMOVE_ESSENTIALS_FILES =
        newProperty("Purge.removeEssentialsFile", false);

    @Comment("World in which the players.dat are stored")
    public static final Property<String> DEFAULT_WORLD =
        newProperty("Purge.defaultWorld", "world");

    @Comment("Remove LimitedCreative/inventories/player.yml, player_creative.yml files during purge?")
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
