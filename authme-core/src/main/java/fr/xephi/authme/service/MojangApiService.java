package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.util.UuidUtils;

import javax.inject.Inject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Centralized HTTP client for Mojang API calls used by the premium feature.
 */
public class MojangApiService {

    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String HAS_JOINED_URL =
        "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private static final Pattern UUID_PATTERN =
        Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(MojangApiService.class);

    @Inject
    MojangApiService() {
    }

    /**
     * Queries the Mojang API to resolve the online UUID for a given username.
     *
     * @param username the player name to look up
     * @return the Mojang online UUID, or empty if the account does not exist or an error occurred
     */
    public Optional<UUID> fetchUuidByName(String username) {
        try {
            HttpURLConnection conn = openGet(PROFILE_URL + username);
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_NOT_FOUND) {
                return Optional.empty();
            }
            if (code == 429) {
                logger.warning("Mojang profile API rate-limited (429) for '" + username + "'; try again later");
                return Optional.empty();
            }
            if (code != HttpURLConnection.HTTP_OK) {
                logger.warning("Mojang profile API returned " + code + " for '" + username + "'");
                return Optional.empty();
            }
            return parseUuid(readBody(conn), username);
        } catch (IOException e) {
            logger.warning("Failed to contact Mojang profile API for '" + username + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Calls Mojang's {@code hasJoined} session endpoint to verify a premium login.
     *
     * @param username   the player name
     * @param serverHash the Minecraft server hash (two's-complement SHA-1 hex)
     * @return the Mojang online UUID on a valid session, or empty on any failure
     */
    public Optional<UUID> hasJoined(String username, String serverHash) {
        try {
            String url = HAS_JOINED_URL + "?username=" + username + "&serverId=" + serverHash;
            HttpURLConnection conn = openGet(url);
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_NOT_FOUND) {
                return Optional.empty();
            }
            if (code != HttpURLConnection.HTTP_OK) {
                logger.warning("Mojang hasJoined returned " + code + " for '" + username + "'");
                return Optional.empty();
            }
            return parseUuid(readBody(conn), username);
        } catch (IOException e) {
            logger.warning("Failed to contact Mojang session server for '" + username + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    private HttpURLConnection openGet(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        return conn;
    }

    private Optional<UUID> parseUuid(String body, String username) {
        Matcher matcher = UUID_PATTERN.matcher(body);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String raw = matcher.group(1);
        String dashed = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-"
            + raw.substring(12, 16) + "-" + raw.substring(16, 20) + "-" + raw.substring(20);
        UUID uuid = UuidUtils.parseUuidSafely(dashed);
        if (uuid == null) {
            logger.warning("Mojang returned an unparseable UUID for '" + username + "': " + raw);
        }
        return Optional.ofNullable(uuid);
    }

    private static String readBody(HttpURLConnection conn) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }
}
