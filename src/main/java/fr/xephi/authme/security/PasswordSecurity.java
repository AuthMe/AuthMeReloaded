package fr.xephi.authme.security;

import ch.jalu.injector.Injector;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.plugin.PluginManager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

/**
 * Manager class for password-related operations.
 */
public class PasswordSecurity implements Reloadable {

    @Inject
    private Settings settings;

    @Inject
    private DataSource dataSource;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private Injector injector;

    private HashAlgorithm algorithm;
    private boolean supportOldAlgorithm;

    /**
     * Load or reload the configuration.
     */
    @PostConstruct
    @Override
    public void reload() {
        this.algorithm = settings.getProperty(SecuritySettings.PASSWORD_HASH);
        this.supportOldAlgorithm = settings.getProperty(SecuritySettings.SUPPORT_OLD_PASSWORD_HASH);
    }

    /**
     * Compute the hash of the configured algorithm for the given password and username.
     *
     * @param password The password to hash
     * @param playerName The player's name
     *
     * @return The password hash
     */
    public HashedPassword computeHash(String password, String playerName) {
        String playerLowerCase = playerName.toLowerCase();
        EncryptionMethod method = initializeEncryptionMethodWithEvent(algorithm, playerLowerCase);
        return method.computeHash(password, playerLowerCase);
    }

    /**
     * Check if the given password matches the player's stored password.
     *
     * @param password The password to check
     * @param playerName The player to check for
     *
     * @return True if the password is correct, false otherwise
     */
    public boolean comparePassword(String password, String playerName) {
        HashedPassword auth = dataSource.getPassword(playerName);
        return auth != null && comparePassword(password, auth, playerName);
    }

    /**
     * Check if the given password matches the given hashed password.
     *
     * @param password The password to check
     * @param hashedPassword The hashed password to check against
     * @param playerName The player to check for
     *
     * @return True if the password matches, false otherwise
     */
    public boolean comparePassword(String password, HashedPassword hashedPassword, String playerName) {
        EncryptionMethod method = initializeEncryptionMethodWithEvent(algorithm, playerName);
        String playerLowerCase = playerName.toLowerCase();
        return methodMatches(method, password, hashedPassword, playerLowerCase)
            || supportOldAlgorithm && compareWithAllEncryptionMethods(password, hashedPassword, playerLowerCase);
    }

    /**
     * Compare the given hash with all available encryption methods to support
     * the migration to a new encryption method. Upon a successful match, the password
     * will be hashed with the new encryption method and persisted.
     *
     * @param password       The clear-text password to check
     * @param hashedPassword The encrypted password to test the clear-text password against
     * @param playerName     The name of the player
     *
     * @return True if there was a password match with another encryption method, false otherwise
     */
    private boolean compareWithAllEncryptionMethods(String password, HashedPassword hashedPassword, String playerName) {
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (!HashAlgorithm.CUSTOM.equals(algorithm)) {
                EncryptionMethod method = initializeEncryptionMethod(algorithm);
                if (methodMatches(method, password, hashedPassword, playerName)) {
                    hashPasswordForNewAlgorithm(password, playerName);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Verify with the given encryption method whether the password matches the hash after checking that
     * the method can be called safely with the given data.
     *
     * @param method The encryption method to use
     * @param password The password to check
     * @param hashedPassword The hash to check against
     * @param playerName The name of the player
     *
     * @return True if the password matched, false otherwise
     */
    private static boolean methodMatches(EncryptionMethod method, String password,
                                         HashedPassword hashedPassword, String playerName) {
        return method != null && (!method.hasSeparateSalt() || hashedPassword.getSalt() != null)
            && method.comparePassword(password, hashedPassword, playerName);
    }

    /**
     * Get the encryption method from the given {@link HashAlgorithm} value and emit a
     * {@link PasswordEncryptionEvent}. The encryption method from the event is then returned,
     * which may have been changed by an external listener.
     *
     * @param algorithm  The algorithm to retrieve the encryption method for
     * @param playerName The name of the player a password will be hashed for
     *
     * @return The encryption method
     */
    private EncryptionMethod initializeEncryptionMethodWithEvent(HashAlgorithm algorithm, String playerName) {
        EncryptionMethod method = initializeEncryptionMethod(algorithm);
        PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
        pluginManager.callEvent(event);
        return event.getMethod();
    }

    /**
     * Initialize the encryption method associated with the given hash algorithm.
     *
     * @param algorithm The algorithm to retrieve the encryption method for
     *
     * @return The associated encryption method, or null if CUSTOM / deprecated
     */
    private EncryptionMethod initializeEncryptionMethod(HashAlgorithm algorithm) {
        if (HashAlgorithm.CUSTOM.equals(algorithm) || HashAlgorithm.PLAINTEXT.equals(algorithm)) {
            return null;
        }
        return injector.newInstance(algorithm.getClazz());
    }

    private void hashPasswordForNewAlgorithm(String password, String playerName) {
        HashedPassword hashedPassword = initializeEncryptionMethodWithEvent(algorithm, playerName)
            .computeHash(password, playerName);
        dataSource.updatePassword(playerName, hashedPassword);
    }

}
