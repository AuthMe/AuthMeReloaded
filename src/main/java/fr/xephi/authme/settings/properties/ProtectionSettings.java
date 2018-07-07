package fr.xephi.authme.settings.properties;

import ch.jalu.configme.Comment;
import ch.jalu.configme.SettingsHolder;
import ch.jalu.configme.properties.Property;

import java.util.List;

import static ch.jalu.configme.properties.PropertyInitializer.newListProperty;
import static ch.jalu.configme.properties.PropertyInitializer.newProperty;


public final class ProtectionSettings implements SettingsHolder {

    @Comment("Enable some servers protection (country based login, antibot)")
    public static final Property<Boolean> ENABLE_PROTECTION =
        newProperty("Protection.enableProtection", false);

    @Comment("Apply the protection also to registered usernames")
    public static final Property<Boolean> ENABLE_PROTECTION_REGISTERED =
        newProperty("Protection.enableProtectionRegistered", true);

    @Comment({
        "Countries allowed to join the server and register. For country codes, see",
        "https://dev.maxmind.com/geoip/legacy/codes/iso3166/",
        "PLEASE USE QUOTES!"})
    public static final Property<List<String>> COUNTRIES_WHITELIST =
        newListProperty("Protection.countries", "US", "GB");

    @Comment({
        "Countries not allowed to join the server and register",
        "PLEASE USE QUOTES!"})
    public static final Property<List<String>> COUNTRIES_BLACKLIST =
        newListProperty("Protection.countriesBlacklist", "A1");

    @Comment("Do we need to enable automatic antibot system?")
    public static final Property<Boolean> ENABLE_ANTIBOT =
        newProperty("Protection.enableAntiBot", true);

    @Comment("The interval in seconds")
    public static final Property<Integer> ANTIBOT_INTERVAL =
        newProperty("Protection.antiBotInterval", 5);

    @Comment({
        "Max number of players allowed to login in the interval",
        "before the AntiBot system is enabled automatically"})
    public static final Property<Integer> ANTIBOT_SENSIBILITY =
        newProperty("Protection.antiBotSensibility", 10);

    @Comment("Duration in minutes of the antibot automatic system")
    public static final Property<Integer> ANTIBOT_DURATION =
        newProperty("Protection.antiBotDuration", 10);

    @Comment("Delay in seconds before the antibot activation")
    public static final Property<Integer> ANTIBOT_DELAY =
        newProperty("Protection.antiBotDelay", 60);

    @Comment("Kicks the player that issued a command before the defined time after the join process")
    public static final Property<Integer> QUICK_COMMANDS_DENIED_BEFORE_MILLISECONDS =
        newProperty("Protection.quickCommands.denyCommandsBeforeMilliseconds", 1000);

    private ProtectionSettings() {
    }

}
