package fr.xephi.authme.security;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.BCRYPT;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;

/**
 */
public class PasswordSecurity {

    public static final HashMap<String, String> userSalt = new HashMap<>();
    private static final SecureRandom rnd = new SecureRandom();

    /**
     * Method createSalt.
     *
     * @param length int
     *
     * @return String * @throws NoSuchAlgorithmException
     */
    public static String createSalt(int length)
        throws NoSuchAlgorithmException {
        byte[] msg = new byte[40];
        rnd.nextBytes(msg);
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        sha1.reset();
        byte[] digest = sha1.digest(msg);
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest)).substring(0, length);
    }

    /**
     * Method getHash.
     *
     * @param alg        HashAlgorithm
     * @param password   String
     * @param playerName String
     *
     * @return String * @throws NoSuchAlgorithmException
     */
    public static String getHash(HashAlgorithm alg, String password,
                                 String playerName) throws NoSuchAlgorithmException {
        EncryptionMethod method;
        try {
            if (alg != HashAlgorithm.CUSTOM)
                method = (EncryptionMethod) alg.getclasse().newInstance();
            else method = null;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new NoSuchAlgorithmException("Problem with this hash algorithm");
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
                return method.getHash(password, null, playerName);
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
        return method.getHash(password, salt, playerName);
    }

    /**
     * Method comparePasswordWithHash.
     *
     * @param password   String
     * @param hash       String
     * @param playerName String
     *
     * @return boolean * @throws NoSuchAlgorithmException
     */
    public static boolean comparePasswordWithHash(String password, String hash,
                                                  String playerName) throws NoSuchAlgorithmException {
        HashAlgorithm algorithm = Settings.getPasswordHash;
        EncryptionMethod method;
        try {
            if (algorithm != HashAlgorithm.CUSTOM)
                method = (EncryptionMethod) algorithm.getclasse().newInstance();
            else
                method = null;

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

    /**
     * Method compareWithAllEncryptionMethod.
     *
     * @param password   String
     * @param hash       String
     * @param playerName String
     *
     * @return boolean * @throws NoSuchAlgorithmException
     */
    private static boolean compareWithAllEncryptionMethod(String password,
                                                          String hash, String playerName) {
        for (HashAlgorithm algo : HashAlgorithm.values()) {
            if (algo != HashAlgorithm.CUSTOM) {
                try {
                    EncryptionMethod method = (EncryptionMethod) algo.getclasse().newInstance();
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
