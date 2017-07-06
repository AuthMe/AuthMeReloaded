package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public final class HooksSettings implements SettingsHolder {

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

    @Comment({
        "-1 means disabled. If you want that only activated players",
        "can log into your server, you can set here the group number",
        "of unactivated users, needed for some forum/CMS support"})
    public static final Property<Integer> NON_ACTIVATED_USERS_GROUP =
        newProperty("ExternalBoardOptions.nonActivedUserGroup", -1);

    @Comment("Other MySQL columns where we need to put the username (case-sensitive)")
    public static final Property<List<String>> MYSQL_OTHER_USERNAME_COLS =
        newListProperty("ExternalBoardOptions.mySQLOtherUsernameColumns");

    @Comment("How much log2 rounds needed in BCrypt (do not change if you do not know what it does)")
    public static final Property<Integer> BCRYPT_LOG2_ROUND =
        newProperty("ExternalBoardOptions.bCryptLog2Round", 10);

    @Comment("phpBB table prefix defined during the phpBB installation process")
    public static final Property<String> PHPBB_TABLE_PREFIX =
        newProperty("ExternalBoardOptions.phpbbTablePrefix", "phpbb_");

    @Comment("phpBB activated group ID; 2 is the default registered group defined by phpBB")
    public static final Property<Integer> PHPBB_ACTIVATED_GROUP_ID =
        newProperty("ExternalBoardOptions.phpbbActivatedGroupId", 2);

    @Comment("IP Board table prefix defined during the IP Board installation process")
    public static final Property<String> IPB_TABLE_PREFIX =
        newProperty("ExternalBoardOptions.IPBTablePrefix", "ipb_");

    @Comment("IP Board default group ID; 3 is the default registered group defined by IP Board")
    public static final Property<Integer> IPB_ACTIVATED_GROUP_ID =
        newProperty("ExternalBoardOptions.IPBActivatedGroupId", 3);

    @Comment("Xenforo table prefix defined during the Xenforo installation process")
    public static final Property<String> XF_TABLE_PREFIX =
        newProperty("ExternalBoardOptions.XFTablePrefix", "xf_");

    @Comment("XenForo default group ID; 2 is the default registered group defined by Xenforo")
    public static final Property<Integer> XF_ACTIVATED_GROUP_ID =
        newProperty("ExternalBoardOptions.XFActivatedGroupId", 2);

    @Comment("Wordpress prefix defined during WordPress installation")
    public static final Property<String> WORDPRESS_TABLE_PREFIX =
        newProperty("ExternalBoardOptions.wordpressTablePrefix", "wp_");


    private HooksSettings() {
    }

}
