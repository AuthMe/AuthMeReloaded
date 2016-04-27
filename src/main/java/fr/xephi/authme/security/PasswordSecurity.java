package fr.xephi.authme.security;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.plugin.PluginManager;

import javax.inject.Inject;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Manager class for password-related operations.
 */
public class PasswordSecurity {

    private final NewSetting settings;
    private HashAlgorithm algorithm;
    private boolean supportOldAlgorithm;
    private final DataSource dataSource;
    private final PluginManager pluginManager;

    @Inject
    public PasswordSecurity(DataSource dataSource, NewSetting settings, PluginManager pluginManager) {
        this.settings = settings;
        this.algorithm = settings.getProperty(SecuritySettings.PASSWORD_HASH);
        this.supportOldAlgorithm = settings.getProperty(SecuritySettings.SUPPORT_OLD_PASSWORD_HASH);
        this.dataSource = dataSource;
        this.pluginManager = pluginManager;
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
     * Reload the configuration.
     */
    public void reload() {
        this.algorithm = settings.getProperty(SecuritySettings.PASSWORD_HASH);
        this.supportOldAlgorithm = settings.getProperty(SecuritySettings.SUPPORT_OLD_PASSWORD_HASH);
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
                EncryptionMethod method = initializeEncryptionMethod(algorithm, settings);
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
        EncryptionMethod method = initializeEncryptionMethod(algorithm, settings);
        PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
        pluginManager.callEvent(event);
        return event.getMethod();
    }

    /**
     * Initialize the encryption method associated with the given hash algorithm.
     *
     * @param algorithm The algorithm to retrieve the encryption method for
     * @param settings  The settings instance to pass to the constructor if required
     *
     * @return The associated encryption method, or null if CUSTOM / deprecated
     */
    public static EncryptionMethod initializeEncryptionMethod(HashAlgorithm algorithm,
                                                              NewSetting settings) {
        try {
            if (HashAlgorithm.CUSTOM.equals(algorithm) || HashAlgorithm.PLAINTEXT.equals(algorithm)) {
                return null;
            }
            Constructor<?> constructor = algorithm.getClazz().getConstructors()[0];
            Class<?>[] parameters = constructor.getParameterTypes();
            if (parameters.length == 0) {
                return (EncryptionMethod) constructor.newInstance();
            } else if (parameters.length == 1 && parameters[0] == NewSetting.class) {
                return (EncryptionMethod) constructor.newInstance(settings);
            } else {
                throw new UnsupportedOperationException("Did not find default constructor or constructor with settings "
                    + "parameter in class " + algorithm.getClazz().getSimpleName());
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new UnsupportedOperationException("Constructor for '" + algorithm.getClazz().getSimpleName()
                + "' could not be invoked. (Is there no default constructor?)", e);
        }
    }

    private void hashPasswordForNewAlgorithm(String password, String playerName) {
        HashedPassword hashedPassword = initializeEncryptionMethodWithEvent(algorithm, playerName)
            .computeHash(password, playerName);
        dataSource.updatePassword(playerName, hashedPassword);
    }

}
