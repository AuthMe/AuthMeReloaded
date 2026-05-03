package fr.xephi.authme.service;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.security.HashUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.OptionalLong;
import java.util.stream.Collectors;

/**
 * Queries the Have I Been Pwned Pwned Passwords range API.
 */
public class PwnedPasswordService {

    private static final String RANGE_API_URL = "https://api.pwnedpasswords.com/range/";
    private static final String USER_AGENT = "AuthMeReloaded";
    private static final int HASH_PREFIX_LENGTH = 5;
    private static final int CONNECT_TIMEOUT_MILLIS = 5_000;
    private static final int READ_TIMEOUT_MILLIS = 5_000;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(PwnedPasswordService.class);

    /**
     * Returns how many times the password appears in the Pwned Passwords database.
     *
     * @param password the password to check
     * @return the count, 0 when absent, or empty if the API could not be queried
     */
    public OptionalLong getPwnedCount(String password) {
        String hash = HashUtils.sha1(password).toUpperCase(Locale.ROOT);
        String hashPrefix = hash.substring(0, HASH_PREFIX_LENGTH);
        String hashSuffix = hash.substring(HASH_PREFIX_LENGTH);

        try {
            return parsePwnedCount(hashSuffix, requestHashRange(hashPrefix));
        } catch (IOException e) {
            logger.debug("Could not query the Pwned Passwords API: {0}", e.getMessage());
            return OptionalLong.empty();
        }
    }

    @VisibleForTesting
    protected String requestHashRange(String hashPrefix) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) URI.create(RANGE_API_URL + hashPrefix)
            .toURL()
            .openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", USER_AGENT);
        connection.setRequestProperty("Add-Padding", "true");
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);

        try {
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP " + responseCode);
            }

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        } finally {
            connection.disconnect();
        }
    }

    private OptionalLong parsePwnedCount(String searchedHashSuffix, String response) {
        String[] entries = response.split("\\R");
        for (String entry : entries) {
            int delimiterIndex = entry.indexOf(':');
            if (delimiterIndex < 0) {
                continue;
            }

            String hashSuffix = entry.substring(0, delimiterIndex);
            if (hashSuffix.equalsIgnoreCase(searchedHashSuffix)) {
                return parseCount(entry.substring(delimiterIndex + 1));
            }
        }
        return OptionalLong.of(0);
    }

    private OptionalLong parseCount(String count) {
        try {
            return OptionalLong.of(Long.parseLong(count.trim()));
        } catch (NumberFormatException e) {
            logger.debug("Could not parse Pwned Passwords count: {0}", count);
            return OptionalLong.empty();
        }
    }
}
