package fr.xephi.authme.settings.properties;

import com.github.authme.configme.Comment;
import com.github.authme.configme.SettingsHolder;
import com.github.authme.configme.properties.Property;

import java.util.List;

import static com.github.authme.configme.properties.PropertyInitializer.newListProperty;
import static com.github.authme.configme.properties.PropertyInitializer.newProperty;


public class ProtectionSettings implements SettingsHolder {

    @Comment("Enable some servers protection (country based login, antibot)")
    public static final Property<Boolean> ENABLE_PROTECTION =
        newProperty("Protection.enableProtection", false);

    @Comment("Apply the protection also to registered usernames")
    public static final Property<Boolean> ENABLE_PROTECTION_REGISTERED =
        newProperty("Protection.enableProtectionRegistered", true);

    @Comment({"Countries allowed to join the server and register, see http://dev.bukkit.org/bukkit-plugins/authme-reloaded/pages/countries-codes/ for countries' codes",
            "PLEASE USE QUOTES!"})
    public static final Property<List<String>> COUNTRIES_WHITELIST =
        newListProperty("Protection.countries", "US", "GB");

    @Comment({"Countries not allowed to join the server and register",
    "PLEASE USE QUOTES!"})
    public static final Property<List<String>> COUNTRIES_BLACKLIST =
        newListProperty("Protection.countriesBlacklist", "A1");

    @Comment("Do we need to enable automatic antibot system?")
    public static final Property<Boolean> ENABLE_ANTIBOT =
        newProperty("Protection.enableAntiBot", true);

    @Comment("Max number of players allowed to login in 5 secs before the AntiBot system is enabled automatically")
    public static final Property<Integer> ANTIBOT_SENSIBILITY =
        newProperty("Protection.antiBotSensibility", 10);

    @Comment("Duration in minutes of the antibot automatic system")
    public static final Property<Integer> ANTIBOT_DURATION =
        newProperty("Protection.antiBotDuration", 10);

    @Comment("Delay in seconds before the antibot activation")
    public static final Property<Integer> ANTIBOT_DELAY =
        newProperty("Protection.antiBotDelay", 60);

    private ProtectionSettings() {
    }

}
