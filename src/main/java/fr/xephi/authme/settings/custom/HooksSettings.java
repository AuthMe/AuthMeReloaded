package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.PropertyType;
import fr.xephi.authme.settings.domain.SettingsClass;

import java.util.List;

import static fr.xephi.authme.settings.domain.Property.newProperty;

public class HooksSettings implements SettingsClass {

    @Comment("Do we need to hook with multiverse for spawn checking?")
    public static final Property<Boolean> MULTIVERSE =
        newProperty("Hooks.multiverse", true);

    @Comment("Do we need to hook with BungeeCord?")
    public static final Property<Boolean> BUNGEECORD =
        newProperty("Hooks.bungeecord", false);

    @Comment("Send player to this BungeeCord server after register/login")
    public static final Property<String> BUNGEECORD_SERVER =
        newProperty("Hooks.sendPlayerTo", "");

    @Comment("Do we need to disable Essentials SocialSpy on join?")
    public static final Property<Boolean> DISABLE_SOCIAL_SPY =
        newProperty("Hooks.disableSocialSpy", false);

    @Comment("Do we need to force /motd Essentials command on join?")
    public static final Property<Boolean> USE_ESSENTIALS_MOTD =
        newProperty("Hooks.useEssentialsMotd", false);

    @Comment("Do we need to cache custom Attributes?")
    public static final Property<Boolean> CACHE_CUSTOM_ATTRIBUTES =
        newProperty("Hooks.customAttributes", false);

    @Comment("These features are only available on VeryGames Server Provider")
    public static final Property<Boolean> ENABLE_VERYGAMES_IP_CHECK =
        newProperty("VeryGames.enableIpCheck", false);

    @Comment({
        "-1 means disabled. If you want that only activated players",
        "can log into your server, you can set here the group number",
        "of unactivated users, needed for some forum/CMS support"})
    public static final Property<Integer> NON_ACTIVATED_USERS_GROUP =
        newProperty("ExternalBoardOptions.nonActivedUserGroup", -1);

    @Comment("Other MySQL columns where we need to put the username (case-sensitive)")
    public static final Property<List<String>> MYSQL_OTHER_USERNAME_COLS =
        newProperty(PropertyType.STRING_LIST, "ExternalBoardOptions.mySQLOtherUsernameColumns");

    @Comment("How much log2 rounds needed in BCrypt (do not change if you do not know what it does)")
    public static final Property<Integer> BCRYPT_LOG2_ROUND =
        newProperty("ExternalBoardOptions.bCryptLog2Round", 10);

    @Comment("phpBB table prefix defined during the phpBB installation process")
    public static final Property<String> PHPBB_TABLE_PREFIX =
        newProperty("ExternalBoardOptions.phpbbTablePrefix", "phpbb_");

    @Comment("phpBB activated group ID; 2 is the default registered group defined by phpBB")
    public static final Property<Integer> PHPBB_ACTIVATED_GROUP_ID =
        newProperty("ExternalBoardOptions.phpbbActivatedGroupId", 2);

    @Comment("Wordpress prefix defined during WordPress installation")
    public static final Property<String> WORDPRESS_TABLE_PREFIX =
        newProperty("ExternalBoardOptions.wordpressTablePrefix", "wp_");

    private HooksSettings() {
    }

}
