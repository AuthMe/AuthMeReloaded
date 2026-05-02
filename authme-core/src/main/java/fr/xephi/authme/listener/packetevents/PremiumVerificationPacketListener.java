package fr.xephi.authme.listener.packetevents;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientEncryptionResponse;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerEncryptionRequest;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PremiumLoginVerifier;
import io.netty.channel.ChannelPipeline;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

/**
 * Intercepts {@code LOGIN_START} and {@code ENCRYPTION_RESPONSE} packets to perform
 * a cryptographic premium identity check against Mojang's session server.
 *
 * <p>For premium players:</p>
 * <ol>
 *   <li>Cancels {@code LOGIN_START} and sends a synthetic {@code EncryptionRequest}.</li>
 *   <li>Cancels {@code ENCRYPTION_RESPONSE}; decrypts and verifies with Mojang
 *       ({@code hasJoined}) on an async thread.</li>
 *   <li>Stores the Mojang UUID in {@link PremiumLoginVerifier} on success.</li>
 *   <li>Re-injects a {@code LOGIN_START} so vanilla processes the login normally.</li>
 * </ol>
 *
 * <p>For non-premium players, all packets pass through unmodified.</p>
 */
public class PremiumVerificationPacketListener extends PacketListenerAbstract {

    private static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(PremiumVerificationPacketListener.class);

    private final DataSource dataSource;
    private final PremiumLoginVerifier loginVerifier;
    private final PendingPremiumCache pendingPremiumCache;

    public PremiumVerificationPacketListener(DataSource dataSource, PremiumLoginVerifier loginVerifier,
                                             PendingPremiumCache pendingPremiumCache) {
        this.dataSource = dataSource;
        this.loginVerifier = loginVerifier;
        this.pendingPremiumCache = pendingPremiumCache;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
            handleLoginStart(event);
        } else if (event.getPacketType() == PacketType.Login.Client.ENCRYPTION_RESPONSE) {
            handleEncryptionResponse(event);
        }
    }

    private void handleLoginStart(PacketReceiveEvent event) {
        WrapperLoginClientLoginStart wrapper = new WrapperLoginClientLoginStart(event);
        String username = wrapper.getUsername();
        if (username == null || !VALID_USERNAME.matcher(username).matches()) {
            return;
        }

        // Capture state before the event is recycled
        User user = event.getUser();
        ClientVersion clientVersion = user.getClientVersion();
        String connectionKey = connectionKey(user);
        // MC >= 1.20.2 includes the player UUID in LOGIN_START; must be forwarded when re-injecting
        UUID playerUUID = wrapper.getPlayerUUID().orElse(null);

        // Cancel immediately so the vanilla server does not process this LOGIN_START yet
        event.setCancelled(true);

        // DB lookup must happen off the Netty event loop to avoid blocking
        CompletableFuture.runAsync(() -> {
            String lowerName = username.toLowerCase(Locale.ROOT);
            fr.xephi.authme.data.auth.PlayerAuth auth = dataSource.getAuth(lowerName);

            boolean isPremium = auth != null && auth.isPremium();
            boolean isPending = !isPremium && pendingPremiumCache.isPending(username);

            if (isPremium || isPending) {
                byte[] verifyToken = loginVerifier.startVerification(connectionKey, username, playerUUID);
                WrapperLoginServerEncryptionRequest encReq = new WrapperLoginServerEncryptionRequest(
                    "", loginVerifier.getPublicKey(), verifyToken, true);
                user.sendPacket(encReq);
            } else {
                // Not premium (and not pending) — resume normal login without verification
                resumeLogin(user, username, clientVersion, playerUUID);
            }
        });
    }

    private void handleEncryptionResponse(PacketReceiveEvent event) {
        User user = event.getUser();
        String connectionKey = connectionKey(user);

        if (!loginVerifier.hasPending(connectionKey)) {
            return;
        }

        String username = loginVerifier.getPendingUsername(connectionKey);
        // Read playerUUID BEFORE completeVerification() removes the pending record
        UUID playerUUID = loginVerifier.getPendingPlayerUUID(connectionKey);
        ClientVersion clientVersion = user.getClientVersion();

        WrapperLoginClientEncryptionResponse wrapper = new WrapperLoginClientEncryptionResponse(event);

        Optional<byte[]> encVerifyTokenOpt = wrapper.getEncryptedVerifyToken();
        if (!encVerifyTokenOpt.isPresent()) {
            // Client responded with a signed nonce (1.19.1+ chat signing). We sent a plain verify
            // token so this should not happen; fall through to password auth without verifying.
            logger.warning("Player '" + username + "' returned a signed nonce during premium "
                + "verification (expected plain verify token). Resuming without premium bypass.");
            loginVerifier.cleanupPending(connectionKey);
            event.setCancelled(true);
            resumeLogin(user, username, clientVersion, playerUUID);
            return;
        }

        byte[] encSharedSecret = wrapper.getEncryptedSharedSecret().clone();
        byte[] encVerifyToken = encVerifyTokenOpt.get().clone();
        event.setCancelled(true);

        // Step 1: RSA-decrypt the shared secret synchronously (fast; we're on the event loop).
        // We must do this here — not inside the async task — so we can set up Netty encryption
        // immediately before returning. The client is already in AES-encrypted mode after
        // sending ENCRYPTION_RESPONSE; without decryption on our side every subsequent packet
        // arrives as garbled bytes and Netty throws "length wider than 21-bit".
        byte[] sharedSecret;
        try {
            sharedSecret = loginVerifier.decryptData(encSharedSecret);
        } catch (GeneralSecurityException e) {
            logger.warning("RSA decryption failed for '" + username + "': " + e.getMessage());
            loginVerifier.cleanupPending(connectionKey);
            resumeLogin(user, username, clientVersion, playerUUID);
            return;
        }

        // Step 2: Install AES cipher handlers in the Netty pipeline synchronously.
        enableChannelEncryption(user.getChannel(), sharedSecret);

        // Step 3: Async — verify the token and call Mojang's hasJoined endpoint.
        loginVerifier.completeVerification(connectionKey, sharedSecret, encVerifyToken)
            .thenAccept(maybeUuid -> {
                maybeUuid.ifPresent(uuid -> loginVerifier.storeVerified(username, uuid));
                resumeLogin(user, username, clientVersion, playerUUID);
            })
            .exceptionally(ex -> {
                logger.warning("Unexpected error during premium verification for '"
                    + username + "': " + ex.getMessage());
                resumeLogin(user, username, clientVersion, playerUUID);
                return null;
            });
    }

    /**
     * Installs AES/CFB8 cipher handlers into the Netty pipeline.
     *
     * <p>Must be called on the channel's event-loop thread (i.e., synchronously from a
     * PacketEvents {@code onPacketReceive} handler).  After the client sends
     * {@code ENCRYPTION_RESPONSE} it immediately encrypts all outbound traffic, so
     * decryption must be in place before any subsequent packet arrives.</p>
     *
     * <p>Handler positions follow vanilla Minecraft's {@code Connection.setupEncryption()}:
     * {@code decrypt} before {@code "splitter"} (decrypt raw bytes before frame-splitting),
     * {@code encrypt} before {@code "prepender"} (encrypt after length-prefixing, since the
     * length varint is inside the encrypted stream in the Minecraft protocol).</p>
     */
    private void enableChannelEncryption(Object channel, byte[] sharedSecret) {
        try {
            SecretKeySpec key = new SecretKeySpec(sharedSecret, "AES");
            // In Minecraft, the IV equals the shared secret (same 16 bytes for key and IV)
            IvParameterSpec iv = new IvParameterSpec(sharedSecret);

            Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            decryptCipher.init(Cipher.DECRYPT_MODE, key, iv);

            Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key, iv);

            ChannelPipeline pipeline = (ChannelPipeline) ChannelHelper.getPipeline(channel);
            pipeline.addBefore("splitter", "decrypt", new AesCfb8Decoder(decryptCipher));
            pipeline.addBefore("prepender", "encrypt", new AesCfb8Encoder(encryptCipher));
        } catch (GeneralSecurityException e) {
            logger.warning("Failed to install AES cipher handlers: " + e.getMessage());
        }
    }

    /**
     * Re-injects a {@code LOGIN_START} packet so vanilla completes the login normally.
     * Uses {@code receivePacketSilently} to bypass PacketEvents' own listener chain
     * (prevents infinite interception of our own injected packet).
     *
     * <p>The {@code playerUUID} must be forwarded for MC >= 1.20.2 clients; it is {@code null}
     * on older protocol versions.</p>
     */
    private void resumeLogin(User user, String username, ClientVersion clientVersion, UUID playerUUID) {
        WrapperLoginClientLoginStart resumePacket =
            new WrapperLoginClientLoginStart(clientVersion, username, null, playerUUID);
        user.receivePacketSilently(resumePacket);
    }

    private static String connectionKey(User user) {
        InetSocketAddress addr = user.getAddress();
        return addr.getAddress().getHostAddress() + ":" + addr.getPort();
    }
}
