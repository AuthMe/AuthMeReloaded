package fr.xephi.authme.security.crypts;

import com.google.common.escape.Escaper;
import com.google.common.io.BaseEncoding;
import com.google.common.net.UrlEscapers;
import com.google.common.primitives.Ints;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

/**
 * Two factor authentication.
 *
 * @see <a href="http://thegreyblog.blogspot.com/2011/12/google-authenticator-using-it-in-your.html">Original source</a>
 */
@Recommendation(Usage.DOES_NOT_WORK)
@HasSalt(SaltType.NONE)
public class TwoFactor extends UnsaltedMethod {

    private static final int SCRET_BYTE = 10;
    private static final int SCRATCH_CODES = 5;
    private static final int BYTES_PER_SCRATCH_CODE = 4;

    private static final int TIME_PRECISION = 3;
    private static final String CRYPTO_ALGO = "HmacSHA1";
    
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(TwoFactor.class);

    /**
     * Creates a link to a QR barcode with the provided secret.
     *
     * @param user the player's name
     * @param host the server host
     * @param secret the TOTP secret
     * @return URL leading to a QR code
     */
    public static String getQrBarcodeUrl(String user, String host, String secret) {
        String format = "https://www.google.com/chart?chs=130x130&chld=M%%7C0&cht=qr&chl="
                + "otpauth://totp/"
                + "%s@%s%%3Fsecret%%3D%s";
        Escaper urlEscaper = UrlEscapers.urlFragmentEscaper();
        return String.format(format, urlEscaper.escape(user), urlEscaper.escape(host), secret);
    }

    @Override
    public String computeHash(String password) {
        // Allocating the buffer
        byte[] buffer = new byte[SCRET_BYTE + SCRATCH_CODES * BYTES_PER_SCRATCH_CODE];

        // Filling the buffer with random numbers.
        // Notice: you want to reuse the same random generator
        // while generating larger random number sequences.
        new SecureRandom().nextBytes(buffer);

        // Getting the key and converting it to Base32
        byte[] secretKey = Arrays.copyOf(buffer, SCRET_BYTE);
        return BaseEncoding.base32().encode(secretKey);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        try {
            return checkPassword(hashedPassword.getHash(), password);
        } catch (Exception e) {
            logger.logException("Failed to verify two auth code:", e);
            return false;
        }
    }

    private boolean checkPassword(String secretKey, String userInput)
            throws NoSuchAlgorithmException, InvalidKeyException {
        Integer code = Ints.tryParse(userInput);
        if (code == null) {
            //code is not an integer
            return false;
        }

        long currentTime = Calendar.getInstance().getTimeInMillis() / TimeUnit.SECONDS.toMillis(30);
        return checkCode(secretKey, code, currentTime);
    }

    private boolean checkCode(String secret, long code, long t) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] decodedKey = BaseEncoding.base32().decode(secret);

        // Window is used to check codes generated in the near past.
        // You can use this value to tune how far you're willing to go.
        int window = TIME_PRECISION;
        for (int i = -window; i <= window; ++i) {
            long hash = verifyCode(decodedKey, t + i);

            if (hash == code) {
                return true;
            }
        }

        // The validation code is invalid.
        return false;
    }

    private int verifyCode(byte[] key, long t) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] data = new byte[8];
        long value = t;
        for (int i = 8; i-- > 0; value >>>= 8) {
            data[i] = (byte) value;
        }

        SecretKeySpec signKey = new SecretKeySpec(key, CRYPTO_ALGO);
        Mac mac = Mac.getInstance(CRYPTO_ALGO);
        mac.init(signKey);
        byte[] hash = mac.doFinal(data);

        int offset = hash[20 - 1] & 0xF;

        // We're using a long because Java hasn't got unsigned int.
        long truncatedHash = 0;
        for (int i = 0; i < 4; ++i) {
            truncatedHash <<= 8;
            // We are dealing with signed bytes:
            // we just keep the first byte.
            truncatedHash |= (hash[offset + i] & 0xFF);
        }

        truncatedHash &= 0x7FFF_FFFF;
        truncatedHash %= 1_000_000;

        return (int) truncatedHash;
    }
}
