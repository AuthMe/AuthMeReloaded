package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;
import fr.xephi.authme.output.LogLevel;

import static ch.jalu.configme.properties.PropertyInitializer.newProperty;

public final class PluginSettings implements SettingsHolder {

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
        "A player's session ends after the timeout or if his IP has changed"
    })
    public static final Property<Integer> SESSIONS_TIMEOUT =
        newProperty("settings.sessions.timeout", 10);

    @Comment({
        "Message language, available languages:",
        "https://github.com/AuthMe/AuthMeReloaded/blob/master/docs/translations.md"
    })
    public static final Property<String> MESSAGES_LANGUAGE =
        newProperty("settings.messagesLanguage", "en");

    @Comment({
        "Enables switching a player to defined permission groups before they log in.",
        "See below for a detailed explanation."
    })
    public static final Property<Boolean> ENABLE_PERMISSION_CHECK =
        newProperty("GroupOptions.enablePermissionCheck", false);

    @Comment({
        "This is a very important option: if a registered player joins the server",
        "AuthMe will switch him to unLoggedInGroup. This should prevent all major exploits.",
        "You can set up your permission plugin with this special group to have no permissions,",
        "or only permission to chat (or permission to send private messages etc.).",
        "The better way is to set up this group with few permissions, so if a player",
        "tries to exploit an account they can do only what you've defined for the group.",
        "After login, the player will be moved to his correct permissions group!",
        "Please note that the group name is case-sensitive, so 'admin' is different from 'Admin'",
        "Otherwise your group will be wiped and the player will join in the default group []!",
        "Example: registeredPlayerGroup: 'NotLogged'"
    })
    public static final Property<String> REGISTERED_GROUP =
        newProperty("GroupOptions.registeredPlayerGroup", "");

    @Comment({
        "Similar to above, unregistered players can be set to the following",
        "permissions group"
    })
    public static final Property<String> UNREGISTERED_GROUP =
        newProperty("GroupOptions.unregisteredPlayerGroup", "");

    @Comment("Forces authme to hook into Vault instead of a specific permission handler system.")
    public static final Property<Boolean> FORCE_VAULT_HOOK =
        newProperty("settings.forceVaultHook", false);

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

    @Comment({
        "By default we handle the AsyncPlayerPreLoginEvent which makes the plugin faster",
        "but it is incompatible with any permission plugin not included in our compatibility list.",
        "If you have issues with permission checks on player join please disable this option."
    })
    public static final Property<Boolean> USE_ASYNC_PRE_LOGIN_EVENT =
        newProperty("settings.useAsyncPreLoginEvent", true);

    private PluginSettings() {
    }

}
