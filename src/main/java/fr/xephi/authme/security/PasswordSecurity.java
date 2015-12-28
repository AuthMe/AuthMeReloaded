package fr.xephi.authme.security;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.BCRYPT;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashResult;
import fr.xephi.authme.security.crypts.NewEncrMethod;
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

    public PasswordSecurity(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Deprecated
    public static String createSalt(int length) {
        return RandomString.generateHex(length);
    }

    @Deprecated
    public static String getHash(HashAlgorithm alg, String password, String playerName) throws NoSuchAlgorithmException {
        EncryptionMethod method;
        try {
            if (alg != HashAlgorithm.CUSTOM)
                method = alg.getClazz().newInstance();
            else method = null;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new NoSuchAlgorithmException("Problem with hash algorithm '" + alg + "'", e);
        }
        String salt = "";
        switch (alg) {
            case SHA256:
                salt = createSalt(16);
                break;
            case MD5VB:
                salt = createSalt(16);
                break;
            case XAUTH:
                salt = createSalt(12);
                break;
            case MYBB:
                salt = createSalt(8);
                userSalt.put(playerName, salt);
                break;
            case IPB3:
                salt = createSalt(5);
                userSalt.put(playerName, salt);
                break;
            case PHPFUSION:
                salt = createSalt(12);
                userSalt.put(playerName, salt);
                break;
            case SALTED2MD5:
                salt = createSalt(Settings.saltLength);
                userSalt.put(playerName, salt);
                break;
            case JOOMLA:
                salt = createSalt(32);
                userSalt.put(playerName, salt);
                break;
            case BCRYPT:
                salt = BCRYPT.gensalt(Settings.bCryptLog2Rounds);
                userSalt.put(playerName, salt);
                break;
            case WBB3:
                salt = createSalt(40);
                userSalt.put(playerName, salt);
                break;
            case WBB4:
                salt = BCRYPT.gensalt(8);
                userSalt.put(playerName, salt);
                break;
            case PBKDF2DJANGO:
            case PBKDF2:
                salt = createSalt(12);
                userSalt.put(playerName, salt);
                break;
            case SMF:
                return method.computeHash(password, null, playerName);
            case PHPBB:
                salt = createSalt(16);
                userSalt.put(playerName, salt);
                break;
            case BCRYPT2Y:
                salt = createSalt(16);
                userSalt.put(playerName, salt);
                break;
            case SALTEDSHA512:
                salt = createSalt(32);
                userSalt.put(playerName, salt);
                break;
            case MD5:
            case SHA1:
            case WHIRLPOOL:
            case PLAINTEXT:
            case XENFORO:
            case SHA512:
            case ROYALAUTH:
            case CRAZYCRYPT1:
            case DOUBLEMD5:
            case WORDPRESS:
            case CUSTOM:
                break;
            default:
                throw new NoSuchAlgorithmException("Unknown hash algorithm");
        }
        PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
        Bukkit.getPluginManager().callEvent(event);
        method = event.getMethod();
        if (method == null)
            throw new NoSuchAlgorithmException("Unknown hash algorithm");
        return method.computeHash(password, salt, playerName);
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

            if (method.comparePassword(hash, password, playerName))
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

    public HashResult computeHash(HashAlgorithm algorithm, String password, String playerName) {
        EncryptionMethod method1 = initializeEncryptionMethod(algorithm, playerName);
        // TODO #358: Remove this check:
        NewEncrMethod method;
        if (method1 instanceof NewEncrMethod) {
            method = (NewEncrMethod) method1;
        } else {
            throw new RuntimeException("TODO #358: Class not yet extended with NewEncrMethod methods");
        }

        return method.computeHash(password, playerName);
    }

    public boolean comparePassword(HashAlgorithm algorithm, String hash, String password, String playerName) {
        EncryptionMethod method1 = initializeEncryptionMethod(algorithm, playerName);
        // TODO #358: Remove this check:
        NewEncrMethod method;
        if (method1 instanceof NewEncrMethod) {
            method = (NewEncrMethod) method1;
        } else {
            throw new RuntimeException("TODO #358: Class not yet extended with NewEncrMethod methods");
        }

        String salt = null;
        if (method.hasSeparateSalt()) {
            PlayerAuth auth = dataSource.getAuth(playerName);
            salt = (auth != null) ? auth.getSalt() : null;
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
                + "' could not be invoked. (Is it public with no arguments?)", e);
        }

        PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
        Bukkit.getPluginManager().callEvent(event);
        return event.getMethod();
    }

    @Deprecated
    private static boolean compareWithAllEncryptionMethod(String password,
                                                          String hash, String playerName) {
        for (HashAlgorithm algo : HashAlgorithm.values()) {
            if (algo != HashAlgorithm.CUSTOM) {
                try {
                    EncryptionMethod method = algo.getClazz().newInstance();
                    if (method.comparePassword(hash, password, playerName)) {
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
