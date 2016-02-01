package fr.xephi.authme.security;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.plugin.PluginManager;

/**
 * Manager class for password-related operations.
 */
public class PasswordSecurity {

    private final DataSource dataSource;
    private final HashAlgorithm algorithm;
    private final PluginManager pluginManager;
    private final boolean supportOldAlgorithm;

    public PasswordSecurity(DataSource dataSource, HashAlgorithm algorithm,
                            PluginManager pluginManager, boolean supportOldAlgorithm) {
        this.dataSource = dataSource;
        this.algorithm = algorithm;
        this.pluginManager = pluginManager;
        this.supportOldAlgorithm = supportOldAlgorithm;
    }

    public HashedPassword computeHash(String password, String playerName) {
        return computeHash(algorithm, password, playerName);
    }

    public HashedPassword computeHash(HashAlgorithm algorithm, String password, String playerName) {
        String playerLowerCase = playerName.toLowerCase();
        EncryptionMethod method = initializeEncryptionMethod(algorithm, playerLowerCase);
        return method.computeHash(password, playerLowerCase);
    }

    public boolean comparePassword(String password, String playerName) {
        HashedPassword auth = dataSource.getPassword(playerName);
        return auth != null && comparePassword(password, auth, playerName);
    }

    public boolean comparePassword(String password, HashedPassword hashedPassword, String playerName) {
        EncryptionMethod method = initializeEncryptionMethod(algorithm, playerName);
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
    private boolean compareWithAllEncryptionMethods(String password, HashedPassword hashedPassword,
                                                    String playerName) {
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (!HashAlgorithm.CUSTOM.equals(algorithm)) {
                EncryptionMethod method = initializeEncryptionMethodWithoutEvent(algorithm);
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
    private EncryptionMethod initializeEncryptionMethod(HashAlgorithm algorithm, String playerName) {
        EncryptionMethod method = initializeEncryptionMethodWithoutEvent(algorithm);
        PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
        pluginManager.callEvent(event);
        return event.getMethod();
    }

    /**
     * Initialize the encryption method corresponding to the given hash algorithm.
     *
     * @param algorithm The algorithm to retrieve the encryption method for
     *
     * @return The associated encryption method
     */
    private static EncryptionMethod initializeEncryptionMethodWithoutEvent(HashAlgorithm algorithm) {
        try {
            return HashAlgorithm.CUSTOM.equals(algorithm)
                ? null
                : algorithm.getClazz().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new UnsupportedOperationException("Constructor for '" + algorithm.getClazz().getSimpleName()
                + "' could not be invoked. (Is there no default constructor?)", e);
        }
    }

    private void hashPasswordForNewAlgorithm(String password, String playerName) {
        HashedPassword hashedPassword = initializeEncryptionMethod(algorithm, playerName)
            .computeHash(password, playerName);
        dataSource.updatePassword(playerName, hashedPassword);
    }

}
