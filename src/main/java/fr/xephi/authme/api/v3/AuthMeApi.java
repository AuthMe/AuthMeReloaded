package fr.xephi.authme.api.v3;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.process.register.executors.ApiPasswordRegisterParams;
import fr.xephi.authme.process.register.executors.RegistrationMethod;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * The current API of AuthMe.
 *
 * Recommended method of retrieving the AuthMeApi object:
 * <code>
 * AuthMeApi authmeApi = AuthMeApi.getInstance();
 * </code>
 */
public class AuthMeApi {

    private static AuthMeApi singleton;
    private final AuthMe plugin;
    private final PluginHookService pluginHookService;
    private final DataSource dataSource;
    private final PasswordSecurity passwordSecurity;
    private final Management management;
    private final ValidationService validationService;
    private final PlayerCache playerCache;
    private final GeoIpService geoIpService;

    /*
     * Constructor for AuthMeApi.
     */
    @Inject
    AuthMeApi(AuthMe plugin, PluginHookService pluginHookService, DataSource dataSource, PlayerCache playerCache,
              PasswordSecurity passwordSecurity, Management management, ValidationService validationService,
              GeoIpService geoIpService) {
        this.plugin = plugin;
        this.pluginHookService = pluginHookService;
        this.dataSource = dataSource;
        this.passwordSecurity = passwordSecurity;
        this.management = management;
        this.validationService = validationService;
        this.playerCache = playerCache;
        this.geoIpService = geoIpService;
        AuthMeApi.singleton = this;
    }

    /**
     * Get the AuthMeApi object for AuthMe.
     *
     * @return The AuthMeApi object, or null if the AuthMe plugin is not enabled or not fully initialized yet
     */
    public static AuthMeApi getInstance() {
        if (singleton != null) {
            return singleton;
        }
        // AuthMeApi is initialized in AuthMe#onEnable -> if singleton is null,
        // it means AuthMe isn't initialized (yet)
        return null;
    }

    /**
     * Return the plugin instance.
     *
     * @return The AuthMe instance
     */
    public AuthMe getPlugin() {
        return plugin;
    }

    /**
     * Gather the version number of the plugin.
     * This can be used to determine whether certain AuthMeApi features are available or not.
     *
     * @return Plugin version identifier as a string.
     */
    public String getPluginVersion() {
        return AuthMe.getPluginVersion();
    }

    /**
     * Return whether the given player is authenticated.
     *
     * @param player The player to verify
     * @return true if the player is authenticated
     */
    public boolean isAuthenticated(Player player) {
        return playerCache.isAuthenticated(player.getName());
    }

    /**
     * Check whether the given player is an NPC.
     *
     * @param player The player to verify
     * @return true if the player is an npc
     */
    public boolean isNpc(Player player) {
        return pluginHookService.isNpc(player);
    }

    /**
     * Check whether the given player is unrestricted. For such players, AuthMe will not require
     * them to authenticate.
     *
     * @param player The player to verify
     * @return true if the player is unrestricted
     * @see fr.xephi.authme.settings.properties.RestrictionSettings#UNRESTRICTED_NAMES
     */
    public boolean isUnrestricted(Player player) {
        return validationService.isUnrestricted(player.getName());
    }

    /**
     * Get the last location of an online player.
     *
     * @param player The player to process
     * @return Location The location of the player
     */
    public Location getLastLocation(Player player) {
        PlayerAuth auth = playerCache.getAuth(player.getName());
        if (auth != null) {
            return new Location(Bukkit.getWorld(auth.getWorld()),
                auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ(), auth.getYaw(), auth.getPitch());
        }
        return null;
    }

    /**
     * Return whether the player is registered.
     *
     * @param playerName The player name to check
     * @return true if player is registered, false otherwise
     */
    public boolean isRegistered(String playerName) {
        String player = playerName.toLowerCase();
        return dataSource.isAuthAvailable(player);
    }

    /**
     * Check the password for the given player.
     *
     * @param playerName      The player to check the password for
     * @param passwordToCheck The password to check
     * @return true if the password is correct, false otherwise
     */
    public boolean checkPassword(String playerName, String passwordToCheck) {
        return passwordSecurity.comparePassword(passwordToCheck, playerName);
    }

    /**
     * Register an OFFLINE/ONLINE player with the given password.
     *
     * @param playerName The player to register
     * @param password   The password to register the player with
     *
     * @return true if the player was registered successfully
     */
    public boolean registerPlayer(String playerName, String password) {
        String name = playerName.toLowerCase();
        HashedPassword result = passwordSecurity.computeHash(password, name);
        if (isRegistered(name)) {
            return false;
        }
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .password(result)
            .realName(playerName)
            .build();
        return dataSource.saveAuth(auth);
    }

    /**
     * Force a player to login, i.e. the player is logged in without needing his password.
     *
     * @param player The player to log in
     */
    public void forceLogin(Player player) {
        management.forceLogin(player);
    }

    /**
     * Force a player to logout.
     *
     * @param player The player to log out
     */
    public void forceLogout(Player player) {
        management.performLogout(player);
    }

    /**
     * Force an ONLINE player to register.
     *
     * @param player    The player to register
     * @param password  The password to use
     * @param autoLogin Should the player be authenticated automatically after the registration?
     */
    public void forceRegister(Player player, String password, boolean autoLogin) {
        management.performRegister(RegistrationMethod.API_REGISTRATION,
            ApiPasswordRegisterParams.of(player, password, autoLogin));
    }

    /**
     * Register an ONLINE player with the given password.
     *
     * @param player   The player to register
     * @param password The password to use
     */
    public void forceRegister(Player player, String password) {
        forceRegister(player, password, true);
    }

    /**
     * Unregister a player from AuthMe.
     *
     * @param player The player to unregister
     */
    public void forceUnregister(Player player) {
        management.performUnregisterByAdmin(null, player.getName(), player);
    }

    /**
     * Unregister a player from AuthMe by name.
     *
     * @param name the name of the player (case-insensitive)
     */
    public void forceUnregister(String name) {
        management.performUnregisterByAdmin(null, name, Bukkit.getPlayer(name));
    }

    /**
     * Get all the registered names (lowercase)
     *
     * @return registered names
     */
    public List<String> getRegisteredNames() {
        List<String> registeredNames = new ArrayList<>();
        dataSource.getAllAuths().forEach(auth -> registeredNames.add(auth.getNickname()));
        return registeredNames;
    }

    /**
     * Get all the registered real-names (original case)
     *
     * @return registered real-names
     */
    public List<String> getRegisteredRealNames() {
        List<String> registeredNames = new ArrayList<>();
        dataSource.getAllAuths().forEach(auth -> registeredNames.add(auth.getRealName()));
        return registeredNames;
    }

    /**
     * Get the country code of the given IP address.
     *
     * @param ip textual IP address to lookup.
     *
     * @return two-character ISO 3166-1 alpha code for the country.
     */
    public String getCountryCode(String ip) {
        return geoIpService.getCountryCode(ip);
    }

    /**
     * Get the country name of the given IP address.
     *
     * @param ip textual IP address to lookup.
     *
     * @return The name of the country.
     */
    public String getCountryName(String ip) {
        return geoIpService.getCountryName(ip);
    }
}
