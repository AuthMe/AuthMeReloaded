package fr.xephi.authme.security;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashResult;
import org.bukkit.Bukkit;

import java.util.HashMap;

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

    public HashResult computeHash(String password, String playerName) {
        return computeHash(algorithm, password, playerName);
    }

    public HashResult computeHash(HashAlgorithm algorithm, String password, String playerName) {
        EncryptionMethod method = initializeEncryptionMethod(algorithm, playerName);
        return method.computeHash(password, playerName);
    }

    public boolean comparePassword(String password, String playerName) {
        PlayerAuth auth = dataSource.getAuth(playerName);
        if (auth != null) {
            return comparePassword(auth.getHash(), auth.getSalt(), password, playerName);
        }
        return false;
    }

    public boolean comparePassword(String hash, String salt, String password, String playerName) {
        EncryptionMethod method = initializeEncryptionMethod(algorithm, playerName);
        // User is not in data source, so the result will invariably be wrong because an encryption
        // method with hasSeparateSalt() == true NEEDS the salt to evaluate the password
        if (method.hasSeparateSalt() && salt == null) {
            return false;
        }
        return method.comparePassword(hash, password, salt, playerName)
            || supportOldAlgorithm && compareWithAllEncryptionMethods(password, hash, salt, playerName);
    }

    /**
     * Compare the given hash with all available encryption methods to support the migration to a new encryption method.
     *
     * @param password   The clear-text password to check
     * @param hash       The hash to text the password against
     * @param salt       The salt (or null if none available)
     * @param playerName The name of the player
     * @return True if the
     */
    private boolean compareWithAllEncryptionMethods(String password, String hash, String salt, String playerName) {
        for (HashAlgorithm algorithm : HashAlgorithm.values()) {
            if (!HashAlgorithm.CUSTOM.equals(algorithm)) {
                EncryptionMethod method = initializeEncryptionMethodWithoutEvent(algorithm);
                if (method != null && method.comparePassword(hash, password, salt, playerName)) {
                    hashPasswordForNewAlgorithm(password, playerName);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get the encryption method from the given {@link HashAlgorithm} value and emits a
     * {@link PasswordEncryptionEvent}. The encryption method from the event is returned,
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
            HashResult hashResult = initializeEncryptionMethod(algorithm, playerName)
                .computeHash(password, playerName);

            // TODO #358: updatePassword() should just take the HashResult..., or at least hash & salt. Idem for setHash
            auth.setSalt(hashResult.getSalt());
            auth.setHash(hashResult.getHash());
            dataSource.updatePassword(auth);
            dataSource.updateSalt(auth);
        }
    }

}
