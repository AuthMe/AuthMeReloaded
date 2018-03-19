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
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.util.PlayerUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
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
    AuthMeApi(AuthMe plugin, DataSource dataSource, PlayerCache playerCache, PasswordSecurity passwordSecurity,
              Management management, ValidationService validationService, GeoIpService geoIpService) {
        this.plugin = plugin;
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
        return singleton;
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
        return PlayerUtils.isNpc(player);
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
     * @return The location of the player
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
     * Get the last ip address of a player.
     *
     * @param playerName The name of the player to process
     * @return The last ip address of the player
     */
    public String getLastIp(String playerName) {
        PlayerAuth auth = playerCache.getAuth(playerName);
        if (auth == null) {
            auth = dataSource.getAuth(playerName);
        }
        if (auth != null) {
            return auth.getLastIp();
        }
        return null;
    }

    /**
     * Get user names by ip.
     *
     * @param address The ip address to process
     * @return The list of user names related to the ip address
     */
    public List<String> getNamesByIp(String address) {
        return dataSource.getAllAuthsByIp(address);
    }

    /**
     * Get the last (AuthMe) login date of a player.
     *
     * @param playerName The name of the player to process
     *
     * @return The date of the last login, or null if the player doesn't exist or has never logged in
     * @deprecated Use Java 8's Instant method {@link #getLastLoginTime(String)}
     */
    @Deprecated
    public Date getLastLogin(String playerName) {
        Long lastLogin = getLastLoginMillis(playerName);
        return lastLogin == null ? null : new Date(lastLogin);
    }

    /**
     * Get the last (AuthMe) login timestamp of a player.
     *
     * @param playerName The name of the player to process
     *
     * @return The timestamp of the last login, or null if the player doesn't exist or has never logged in
     */
    public Instant getLastLoginTime(String playerName) {
        Long lastLogin = getLastLoginMillis(playerName);
        return lastLogin == null ? null : Instant.ofEpochMilli(lastLogin);
    }

    private Long getLastLoginMillis(String playerName) {
        PlayerAuth auth = playerCache.getAuth(playerName);
        if (auth == null) {
            auth = dataSource.getAuth(playerName);
        }
        if (auth != null) {
            return auth.getLastLogin();
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
        if (isRegistered(name)) {
            return false;
        }
        HashedPassword result = passwordSecurity.computeHash(password, name);
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .password(result)
            .realName(playerName)
            .registrationDate(System.currentTimeMillis())
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
     * Change a user's password
     *
     * @param name the user name
     * @param newPassword the new password
     */
    public void changePassword(String name, String newPassword) {
        management.performPasswordChangeAsAdmin(null, name, newPassword);
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
