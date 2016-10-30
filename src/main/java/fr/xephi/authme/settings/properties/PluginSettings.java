package fr.xephi.authme.settings.properties;

import com.github.authme.configme.Comment;
import com.github.authme.configme.SettingsHolder;
import com.github.authme.configme.properties.Property;
import fr.xephi.authme.output.LogLevel;

import static com.github.authme.configme.properties.PropertyInitializer.newProperty;

public class PluginSettings implements SettingsHolder {

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

    @Comment({
        "Message language, available languages:",
        "https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/translations.md"
    })
    public static final Property<String> MESSAGES_LANGUAGE =
        newProperty("settings.messagesLanguage", "en");

    @Comment({
        "Take care with this option; if you want",
        "to use group switching of AuthMe",
        "for unloggedIn players, set this setting to true.",
        "Default is false."
    })
    public static final Property<Boolean> ENABLE_PERMISSION_CHECK =
        newProperty("permission.EnablePermissionCheck", false);

    @Comment({
        "Keeps collisions disabled for logged players",
        "Works only with MC 1.9"
    })
    public static final Property<Boolean> KEEP_COLLISIONS_DISABLED =
        newProperty("settings.restrictions.keepCollisionsDisabled", false);

    @Comment({
        "Log level: INFO, FINE, DEBUG. Use INFO for general messages,",
        "FINE for some additional detailed ones (like password failed),",
        "and DEBUG for debugging"
    })
    public static final Property<LogLevel> LOG_LEVEL =
        newProperty(LogLevel.class, "settings.logLevel", LogLevel.FINE);

    @Comment({
        "By default we schedule async tasks when talking to the database. If you want",
        "typical communication with the database to happen synchronously, set this to false"
    })
    public static final Property<Boolean> USE_ASYNC_TASKS =
        newProperty("settings.useAsyncTasks", true);

    private PluginSettings() {
    }

}
