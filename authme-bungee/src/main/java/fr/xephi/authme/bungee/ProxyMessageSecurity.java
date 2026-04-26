package fr.xephi.authme.bungee;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

final class ProxyMessageSecurity {

    private static final String HMAC_ALGO = "HmacSHA256";
    static final long MAX_AGE_MILLIS = 30_000L;

    private ProxyMessageSecurity() {
    }

    static String computeHmac(String secret, String playerName, long timestamp) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hmacBytes = mac.doFinal((playerName + ":" + timestamp).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    static boolean verifyHmac(String secret, String playerName, long timestamp, String providedHmac) {
        if (Math.abs(System.currentTimeMillis() - timestamp) > MAX_AGE_MILLIS) {
            return false;
        }
        String expectedHmac = computeHmac(secret, playerName, timestamp);
        return MessageDigest.isEqual(
            expectedHmac.getBytes(StandardCharsets.UTF_8),
            providedHmac.getBytes(StandardCharsets.UTF_8));
    }
}
