package fr.xephi.authme.velocity.premium;

import javax.crypto.Cipher;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class ProxyPremiumLoginVerifier {

    private static final String HAS_JOINED_URL =
        "https://sessionserver.mojang.com/session/minecraft/hasJoined";
    private static final Pattern UUID_PATTERN =
        Pattern.compile("\"id\"\\s*:\\s*\"([0-9a-fA-F]{32})\"");
    private static final long VERIFIED_TTL_MS = 60_000L;
    private static final long PENDING_TTL_MS = 30_000L;

    private final KeyPair rsaKeyPair;
    private final SecureRandom secureRandom;
    private final Consumer<String> warningLogger;
    private final ExecutorService verificationExecutor;
    private final ConcurrentHashMap<String, PendingVerification> pending = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, VerifiedSession> verified = new ConcurrentHashMap<>();

    ProxyPremiumLoginVerifier(String threadName, Consumer<String> warningLogger) {
        this.warningLogger = warningLogger;
        this.secureRandom = new SecureRandom();
        this.verificationExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, threadName);
            thread.setDaemon(true);
            return thread;
        });
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(1024, secureRandom);
            this.rsaKeyPair = gen.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("RSA algorithm not available", e);
        }
    }

    PublicKey getPublicKey() {
        return rsaKeyPair.getPublic();
    }

    byte[] startVerification(String connectionKey, String username, UUID playerUuid, boolean pendingPremiumEnrollment) {
        evictStalePendingEntries();
        verified.remove(username.toLowerCase(Locale.ROOT));
        byte[] verifyToken = new byte[4];
        secureRandom.nextBytes(verifyToken);
        pending.put(connectionKey,
            new PendingVerification(username, playerUuid, verifyToken, System.currentTimeMillis(), pendingPremiumEnrollment));
        return verifyToken;
    }

    boolean hasPending(String connectionKey) {
        return pending.containsKey(connectionKey);
    }

    String getPendingUsername(String connectionKey) {
        PendingVerification pendingVerification = pending.get(connectionKey);
        return pendingVerification != null ? pendingVerification.username() : null;
    }

    UUID getPendingPlayerUuid(String connectionKey) {
        PendingVerification pendingVerification = pending.get(connectionKey);
        return pendingVerification != null ? pendingVerification.playerUuid() : null;
    }

    boolean isPendingPremiumEnrollment(String connectionKey) {
        PendingVerification pendingVerification = pending.get(connectionKey);
        return pendingVerification != null && pendingVerification.pendingPremiumEnrollment();
    }

    void cleanupPending(String connectionKey) {
        pending.remove(connectionKey);
    }

    byte[] decryptData(byte[] encrypted) throws GeneralSecurityException {
        return rsaDecrypt(encrypted);
    }

    CompletableFuture<Optional<UUID>> completeVerification(
        String connectionKey, byte[] sharedSecret, byte[] encryptedVerifyToken) {
        PendingVerification pendingVerification = pending.remove(connectionKey);
        if (pendingVerification == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] decryptedToken = rsaDecrypt(encryptedVerifyToken);
                if (!Arrays.equals(decryptedToken, pendingVerification.verifyToken())) {
                    warningLogger.accept("Proxy premium verification failed for '" + pendingVerification.username()
                        + "': verify token mismatch");
                    return Optional.empty();
                }
                String serverHash = computeServerHash(sharedSecret);
                return hasJoined(pendingVerification.username(), serverHash);
            } catch (Exception e) {
                warningLogger.accept("Proxy premium verification failed for '" + pendingVerification.username()
                    + "': " + e.getMessage());
                return Optional.empty();
            }
        }, verificationExecutor);
    }

    void storeVerified(String username, UUID mojangUuid) {
        verified.put(username.toLowerCase(Locale.ROOT), new VerifiedSession(mojangUuid, System.currentTimeMillis()));
    }

    UUID getVerifiedUuid(String username) {
        String normalizedName = username.toLowerCase(Locale.ROOT);
        VerifiedSession verifiedSession = verified.get(normalizedName);
        if (verifiedSession == null) {
            return null;
        }
        if (System.currentTimeMillis() - verifiedSession.verifiedAt() > VERIFIED_TTL_MS) {
            verified.remove(normalizedName);
            return null;
        }
        return verifiedSession.mojangUuid();
    }

    void clearVerified(String username) {
        verified.remove(username.toLowerCase(Locale.ROOT));
    }

    void shutdown() {
        verificationExecutor.shutdownNow();
        pending.clear();
        verified.clear();
    }

    private void evictStalePendingEntries() {
        long now = System.currentTimeMillis();
        pending.entrySet().removeIf(entry -> now - entry.getValue().startedAt() > PENDING_TTL_MS);
    }

    private byte[] rsaDecrypt(byte[] encrypted) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, rsaKeyPair.getPrivate());
        return cipher.doFinal(encrypted);
    }

    private String computeServerHash(byte[] sharedSecret) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        sha1.update("".getBytes(StandardCharsets.ISO_8859_1));
        sha1.update(sharedSecret);
        sha1.update(rsaKeyPair.getPublic().getEncoded());
        return new BigInteger(sha1.digest()).toString(16);
    }

    private Optional<UUID> hasJoined(String username, String serverHash) {
        try {
            HttpURLConnection connection = openGet(HAS_JOINED_URL + "?username=" + username + "&serverId=" + serverHash);
            int code = connection.getResponseCode();
            if (code == HttpURLConnection.HTTP_NO_CONTENT || code == HttpURLConnection.HTTP_NOT_FOUND) {
                return Optional.empty();
            }
            if (code != HttpURLConnection.HTTP_OK) {
                warningLogger.accept("Mojang hasJoined returned " + code + " for '" + username + "'");
                return Optional.empty();
            }
            return parseUuid(readBody(connection), username);
        } catch (IOException e) {
            warningLogger.accept("Failed to contact Mojang session server for '" + username + "': " + e.getMessage());
            return Optional.empty();
        }
    }

    private HttpURLConnection openGet(String urlString) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(urlString).openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        return connection;
    }

    private Optional<UUID> parseUuid(String body, String username) {
        Matcher matcher = UUID_PATTERN.matcher(body);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String raw = matcher.group(1);
        String dashed = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-"
            + raw.substring(12, 16) + "-" + raw.substring(16, 20) + "-" + raw.substring(20);
        try {
            return Optional.of(UUID.fromString(dashed));
        } catch (IllegalArgumentException e) {
            warningLogger.accept("Mojang returned an unparseable UUID for '" + username + "': " + raw);
            return Optional.empty();
        }
    }

    private static String readBody(HttpURLConnection connection) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private record PendingVerification(
        String username, UUID playerUuid, byte[] verifyToken, long startedAt, boolean pendingPremiumEnrollment) {
    }

    private record VerifiedSession(UUID mojangUuid, long verifiedAt) {
    }
}
