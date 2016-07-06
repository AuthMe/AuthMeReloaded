package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

/**
 * Old settings manager. See {@link NewSetting} for the new manager.
 */
@Deprecated
public final class Settings {

    public static List<String> getUnrestrictedName;
    public static boolean isAllowRestrictedIp;
    public static boolean isStopEnabled;
    public static boolean reloadSupport;
    public static String getUnloggedinGroup;
    public static String unRegisteredGroup;
    public static String getRegisteredGroup;
    public static int getNonActivatedGroup;
    private static FileConfiguration configFile;

    /**
     * Constructor for Settings.
     *
     * @param pl AuthMe
     */
    public Settings(AuthMe pl) {
        configFile = pl.getConfig();
        loadVariables();
    }

    private static void loadVariables() {
        isAllowRestrictedIp = load(RestrictionSettings.ENABLE_RESTRICTED_USERS);
        getUnloggedinGroup = load(SecuritySettings.UNLOGGEDIN_GROUP);
        getNonActivatedGroup = configFile.getInt("ExternalBoardOptions.nonActivedUserGroup", -1);
        unRegisteredGroup = configFile.getString("GroupOptions.UnregisteredPlayerGroup", "");
        getUnrestrictedName = load(RestrictionSettings.UNRESTRICTED_NAMES);
        getRegisteredGroup = configFile.getString("GroupOptions.RegisteredPlayerGroup", "");
        isStopEnabled = configFile.getBoolean("Security.SQLProblem.stopServer", true);
        reloadSupport = configFile.getBoolean("Security.ReloadCommand.useReloadCommandSupport", true);
    }

    /**
     * Load the value via the new Property setup for temporary support within this old settings manager.
     *
     * @param property The property to load
     * @param <T> The property type
     * @return The config value of the property
     */
    private static <T> T load(Property<T> property) {
        return property.getFromFile(configFile);
    }
}
