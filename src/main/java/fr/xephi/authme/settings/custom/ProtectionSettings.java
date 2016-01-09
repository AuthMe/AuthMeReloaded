package fr.xephi.authme.settings.custom;

import fr.xephi.authme.settings.domain.Comment;
import fr.xephi.authme.settings.domain.Property;
import fr.xephi.authme.settings.domain.SettingsClass;

import java.util.List;

import static fr.xephi.authme.settings.domain.Property.newProperty;
import static fr.xephi.authme.settings.domain.PropertyType.BOOLEAN;
import static fr.xephi.authme.settings.domain.PropertyType.INTEGER;
import static fr.xephi.authme.settings.domain.PropertyType.STRING_LIST;


public class ProtectionSettings implements SettingsClass {

    @Comment("Enable some servers protection (country based login, antibot)")
    public static final Property<Boolean> ENABLE_PROTECTION =
        newProperty(BOOLEAN, "Protection.enableProtection", false);

    @Comment({"Countries allowed to join the server and register, see http://dev.bukkit.org/bukkit-plugins/authme-reloaded/pages/countries-codes/ for countries' codes",
            "PLEASE USE QUOTES!"})
    public static final Property<List<String>> COUNTRIES_WHITELIST =
        newProperty(STRING_LIST, "Protection.countries", "US", "GB", "A1");

    @Comment({"Countries not allowed to join the server and register",
    "PLEASE USE QUOTES!"})
    public static final Property<List<String>> COUNTRIES_BLACKLIST =
        newProperty(STRING_LIST, "Protection.countriesBlacklist");

    @Comment("Do we need to enable automatic antibot system?")
    public static final Property<Boolean> ENABLE_ANTIBOT =
        newProperty(BOOLEAN, "Protection.enableAntiBot", false);

    @Comment("Max number of player allowed to login in 5 secs before enable AntiBot system automatically")
    public static final Property<Integer> ANTIBOT_SENSIBILITY =
        newProperty(INTEGER, "Protection.antiBotSensibility", 5);

    @Comment("Duration in minutes of the antibot automatic system")
    public static final Property<Integer> ANTIBOT_DURATION =
        newProperty(INTEGER, "Protection.antiBotDuration", 10);

    private ProtectionSettings() {
    }

}
