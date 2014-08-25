package fr.xephi.authme.security;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;

import org.bukkit.Bukkit;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.events.PasswordEncryptionEvent;
import fr.xephi.authme.security.crypts.BCRYPT;
import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.settings.Settings;

public class PasswordSecurity {

    private static SecureRandom rnd = new SecureRandom();
    public static HashMap<String, String> userSalt = new HashMap<String, String>();

    public static String createSalt(int length) throws NoSuchAlgorithmException {
        byte[] msg = new byte[40];
        rnd.nextBytes(msg);
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        sha1.reset();
        byte[] digest = sha1.digest(msg);
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest)).substring(0, length);
    }

    public static String getHash(HashAlgorithm alg, String password,
            String playerName) throws NoSuchAlgorithmException {
        EncryptionMethod method;
        try {
            if (alg != HashAlgorithm.CUSTOM)
                method = (EncryptionMethod) alg.getclass().newInstance();
            else method = null;
        } catch (InstantiationException e) {
            throw new NoSuchAlgorithmException("Problem with this hash algorithm");
        } catch (IllegalAccessException e) {
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

    public static boolean comparePasswordWithHash(String password, String hash,
            String playerName) throws NoSuchAlgorithmException {
        HashAlgorithm algo = Settings.getPasswordHash;
        EncryptionMethod method;
        try {
            if (algo != HashAlgorithm.CUSTOM)
                method = (EncryptionMethod) algo.getclass().newInstance();
            else method = null;
        } catch (InstantiationException e) {
            throw new NoSuchAlgorithmException("Problem with this hash algorithm");
        } catch (IllegalAccessException e) {
            throw new NoSuchAlgorithmException("Problem with this hash algorithm");
        }
        PasswordEncryptionEvent event = new PasswordEncryptionEvent(method, playerName);
        Bukkit.getPluginManager().callEvent(event);
        method = event.getMethod();
        if (method == null)
            throw new NoSuchAlgorithmException("Unknown hash algorithm");

        try {
            if (method.comparePassword(hash, password, playerName))
                return true;
        } catch (Exception e) {
        }
        if (Settings.supportOldPassword) {
            try {
                if (compareWithAllEncryptionMethod(password, hash, playerName))
                    return true;
            } catch (Exception e) {
            }
        }
        return false;
    }

    private static boolean compareWithAllEncryptionMethod(String password,
            String hash, String playerName) throws NoSuchAlgorithmException {
        for (HashAlgorithm algo : HashAlgorithm.values()) {
            if (algo != HashAlgorithm.CUSTOM)
                try {
                    EncryptionMethod method = (EncryptionMethod) algo.getclass().newInstance();
                    if (method.comparePassword(hash, password, playerName)) {
                        PlayerAuth nAuth = AuthMe.getInstance().database.getAuth(playerName);
                        if (nAuth != null) {
                            nAuth.setHash(getHash(Settings.getPasswordHash, password, playerName));
                            nAuth.setSalt(userSalt.get(playerName));
                            AuthMe.getInstance().database.updatePassword(nAuth);
                            AuthMe.getInstance().database.updateSalt(nAuth);
                        }
                        return true;
                    }
                } catch (Exception e) {
                }
        }
        return false;
    }
}
