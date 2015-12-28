package fr.xephi.authme.security;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashResult;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 */
public class PasswordSecurity {

    @Deprecated
    public static final HashMap<String, String> userSalt = new HashMap<>();
    private final DataSource dataSource;
    private final HashAlgorithm algorithm;
    private final boolean supportOldAlgorithm;

    public PasswordSecurity(DataSource dataSource, HashAlgorithm algorithm, boolean supportOldAlgorithm) {
        this.dataSource = dataSource;
        this.algorithm = algorithm;
        this.supportOldAlgorithm = supportOldAlgorithm;
    }

    @Deprecated
    public static String createSalt(int length) {
        return RandomString.generateHex(length);
    }

    @Deprecated
    public static String getHash(HashAlgorithm alg, String password, String playerName) throws NoSuchAlgorithmException {
        return "";
    }

    @Deprecated
    public static boolean comparePasswordWithHash(String password, String hash,
                                                  String playerName) throws NoSuchAlgorithmException {
        HashAlgorithm algorithm = Settings.getPasswordHash;
        EncryptionMethod method;
        try {
            if (algorithm != HashAlgorithm.CUSTOM) {
                method = algorithm.getClazz().newInstance();
            } else {
                method = null;
            }

            PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
            Bukkit.getPluginManager().callEvent(event);
            method = event.getMethod();

            if (method == null)
                throw new NoSuchAlgorithmException("Unknown hash algorithm");

            String salt = null;
            if (method.hasSeparateSalt()) {
                PlayerAuth auth = AuthMe.getInstance().getDataSource().getAuth(playerName);
                if (auth == null) {
                    // User is not in data source, so the result will invariably be wrong because an encryption
                    // method with hasSeparateSalt() == true NEEDS the salt to evaluate the password
                    return false;
                }
                salt = auth.getSalt();
            }

            if (method.comparePassword(hash, password, salt, playerName))
                return true;

            if (Settings.supportOldPassword) {
                if (compareWithAllEncryptionMethod(password, hash, playerName))
                    return true;
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new NoSuchAlgorithmException("Problem with this hash algorithm");
        }
        return false;
    }

    public HashResult computeHash(String password, String playerName) {
        return computeHash(algorithm, password, playerName);
    }

    public HashResult computeHash(HashAlgorithm algorithm, String password, String playerName) {
        EncryptionMethod method = initializeEncryptionMethod(algorithm, playerName);
        return method.computeHash(password, playerName);
    }

    public boolean comparePassword(String hash, String password, String playerName) {
        return comparePassword(algorithm, hash, password, playerName);
    }

    public boolean comparePassword(HashAlgorithm algorithm, String hash, String password, String playerName) {
        EncryptionMethod method = initializeEncryptionMethod(algorithm, playerName);
        String salt = null;
        if (method.hasSeparateSalt()) {
            PlayerAuth auth = dataSource.getAuth(playerName);
            if (auth == null) {
                // User is not in data source, so the result will invariably be wrong because an encryption
                // method with hasSeparateSalt() == true NEEDS the salt to evaluate the password
                return false;
            }
            salt = auth.getSalt();
        }
        return method.comparePassword(hash, password, salt, playerName);
        // TODO #358: Add logic for Settings.supportOldPassword
    }

    private EncryptionMethod initializeEncryptionMethod(HashAlgorithm algorithm, String playerName) {
        EncryptionMethod method;
        try {
            method = HashAlgorithm.CUSTOM.equals(algorithm)
                ? null
                : algorithm.getClazz().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException("Constructor for '" + algorithm.getClazz()
                + "' could not be invoked. (Is there no default constructor?)", e);
        }

        PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
        Bukkit.getPluginManager().callEvent(event);
        return event.getMethod();
    }

    @Deprecated
    private static boolean compareWithAllEncryptionMethod(String password,
                                                          String hash, String playerName) {
        String salt;
        PlayerAuth auth = AuthMe.getInstance().getDataSource().getAuth(playerName);
        if (auth == null) {
            salt = null;
        } else {
            salt = auth.getSalt();
        }

        for (HashAlgorithm algo : HashAlgorithm.values()) {
            if (algo != HashAlgorithm.CUSTOM) {
                try {
                    EncryptionMethod method = algo.getClazz().newInstance();
                    if (method.comparePassword(hash, password, salt, playerName)) {
                        PlayerAuth nAuth = AuthMe.getInstance().database.getAuth(playerName);
                        if (nAuth != null) {
                            nAuth.setHash(getHash(Settings.getPasswordHash, password, playerName));
                            nAuth.setSalt(userSalt.containsKey(playerName) ? userSalt.get(playerName) : "");
                            AuthMe.getInstance().database.updatePassword(nAuth);
                            AuthMe.getInstance().database.updateSalt(nAuth);
                        }
                        return true;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return false;
    }
}
