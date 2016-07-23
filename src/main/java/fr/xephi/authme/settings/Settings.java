package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Old settings manager. See {@link NewSetting} for the new manager.
 */
@Deprecated
public final class Settings {

    public static String unRegisteredGroup;
    public static String getRegisteredGroup;

    /**
     * Constructor for Settings.
     *
     * @param pl AuthMe
     */
    public Settings(AuthMe pl) {
        FileConfiguration configFile = pl.getConfig();
        unRegisteredGroup = configFile.getString("GroupOptions.UnregisteredPlayerGroup", "");
        getRegisteredGroup = configFile.getString("GroupOptions.RegisteredPlayerGroup", "");
    }
}
