package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;

import static fr.xephi.authme.settings.domain.Property.newProperty;

public class PluginSettings implements SettingsClass {

    @Comment("The name shown in the help messages")
    public static final Property<String> HELP_HEADER =
        newProperty("settings.helpHeader", "AuthMeReloaded");

    @Comment({
        "Do you want to enable the session feature?",
        "If enabled, when a player authenticates successfully,",
        "his IP and his nickname is saved.",
        "The next time the player joins the server, if his IP",
        "is the same as last time and the timeout hasn't",
        "expired, he will not need to authenticate."
    })
    public static final Property<Boolean> SESSIONS_ENABLED =
        newProperty("settings.sessions.enabled", false);

    @Comment({
        "After how many minutes should a session expire?",
        "0 for unlimited time (Very dangerous, use it at your own risk!)",
        "Remember that sessions will end only after the timeout, and",
        "if the player's IP has changed but the timeout hasn't expired,",
        "the player will be kicked from the server due to invalid session"
    })
    public static final Property<Integer> SESSIONS_TIMEOUT =
        newProperty("settings.sessions.timeout", 10);

    @Comment({
        "Should the session expire if the player tries to log in with",
        "another IP address?"
    })
    public static final Property<Boolean> SESSIONS_EXPIRE_ON_IP_CHANGE =
        newProperty("settings.sessions.sessionExpireOnIpChange", true);

    @Comment("Message language, available: en, de, br, cz, pl, fr, ru, hu, sk, es, zhtw, fi, zhcn, lt, it, ko, pt")
    public static final Property<String> MESSAGES_LANGUAGE =
        newProperty("settings.messagesLanguage", "en");

    @Comment({
        "Take care with this option; if you don't want",
        "to use Vault and group switching of AuthMe",
        "for unloggedIn players, set this setting to true.",
        "Default is false."
    })
    public static final Property<Boolean> ENABLE_PERMISSION_CHECK =
        newProperty("permission.EnablePermissionCheck", false);


    private PluginSettings() {
    }

}
