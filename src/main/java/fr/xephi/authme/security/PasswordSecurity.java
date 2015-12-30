package fr.xephi.authme.security;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.EncryptedPassword;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import org.bukkit.Bukkit;

/**
 * Manager class for password-related operations.
 */
public class PasswordSecurity {

    private final DataSource dataSource;
    private final HashAlgorithm algorithm;
    private final boolean supportOldAlgorithm;

    public PasswordSecurity(DataSource dataSource, HashAlgorithm algorithm, boolean supportOldAlgorithm) {
        this.dataSource = dataSource;
        this.algorithm = algorithm;
        this.supportOldAlgorithm = supportOldAlgorithm;
    }

    public EncryptedPassword computeHash(String password, String playerName) {
        return computeHash(algorithm, password, playerName);
    }

    public EncryptedPassword computeHash(HashAlgorithm algorithm, String password, String playerName) {
        EncryptionMethod method = initializeEncryptionMethod(algorithm, playerName);
        return method.computeHash(password, playerName);
    }

    public boolean comparePassword(String password, String playerName) {
        // TODO ljacqu 20151230: Defining a dataSource.getPassword() method would be more efficient
        PlayerAuth auth = dataSource.getAuth(playerName);
        if (auth != null) {
            return comparePassword(password, auth.getPassword(), playerName);
        }
        return false;
    }

    public boolean comparePassword(String password, EncryptedPassword encryptedPassword, String playerName) {
        EncryptionMethod method = initializeEncryptionMethod(algorithm, playerName);
        // User is not in data source, so the result will invariably be wrong because an encryption
        // method with hasSeparateSalt() == true NEEDS the salt to evaluate the password
        String salt = encryptedPassword.getSalt();
        if (method.hasSeparateSalt() && salt == null) {
            return false;
        }

        return method.comparePassword(password, encryptedPassword, playerName)
            || supportOldAlgorithm && compareWithAllEncryptionMethods(password, encryptedPassword, playerName);
    }

    /**
     * Compare the given hash with all available encryption methods to support
     * the migration to a new encryption method. Upon a successful match, the password
     * will be hashed with the new encryption method and persisted.
     *
     * @param password          The clear-text password to check
     * @param encryptedPassword The encrypted password to test the clear-text password against
     * @param playerName        The name of the player
     * @return True if the
     */
    private boolean compareWithAllEncryptionMethods(String password, EncryptedPassword encryptedPassword,
                                                    String playerName) {
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (!HashAlgorithm.CUSTOM.equals(algorithm)) {
                EncryptionMethod method = initializeEncryptionMethodWithoutEvent(algorithm);
                if (method != null && method.comparePassword(password, encryptedPassword, playerName)) {
                    hashPasswordForNewAlgorithm(password, playerName);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the encryption method from the given {@link HashAlgorithm} value and emit a
     * {@link PasswordEncryptionEvent}. The encryption method from the event is then returned,
     * which may have been changed by an external listener.
     *
     * @param algorithm The algorithm to retrieve the encryption method for
     * @param playerName The name of the player a password will be hashed for
     * @return The encryption method
     */
    private static EncryptionMethod initializeEncryptionMethod(HashAlgorithm algorithm, String playerName) {
        EncryptionMethod method = initializeEncryptionMethodWithoutEvent(algorithm);
        PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
        Bukkit.getPluginManager().callEvent(event);
        return event.getMethod();
    }

    /**
     * Initialize the encryption method corresponding to the given hash algorithm.
     *
     * @param algorithm The algorithm to retrieve the encryption method for
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
        PlayerAuth auth = dataSource.getAuth(playerName);
        if (auth != null) {
            EncryptedPassword encryptedPassword = initializeEncryptionMethod(algorithm, playerName)
                .computeHash(password, playerName);
            auth.setPassword(encryptedPassword);
            dataSource.updatePassword(auth);
        }
    }

}
