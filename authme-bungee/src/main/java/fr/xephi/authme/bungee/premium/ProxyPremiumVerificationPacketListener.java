package fr.xephi.authme.bungee.premium;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.netty.channel.ChannelHelper;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.player.ClientVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientEncryptionResponse;
import com.github.retrooper.packetevents.wrapper.login.client.WrapperLoginClientLoginStart;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerEncryptionRequest;
import io.netty.channel.ChannelPipeline;
import io.netty.util.ReferenceCountUtil;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

final class ProxyPremiumVerificationPacketListener extends PacketListenerAbstract {

    private static final Pattern VALID_USERNAME = Pattern.compile("^[a-zA-Z0-9_]{2,16}$");

    private final Predicate<String> requiresVerification;
    private final Predicate<String> isPendingVerification;
    private final Consumer<String> pendingVerificationFailureHandler;
    private final ProxyPremiumLoginVerifier loginVerifier;
    private final Consumer<String> warningLogger;

    ProxyPremiumVerificationPacketListener(Predicate<String> requiresVerification,
                                           Predicate<String> isPendingVerification,
                                           Consumer<String> pendingVerificationFailureHandler,
                                           ProxyPremiumLoginVerifier loginVerifier,
                                           Consumer<String> warningLogger) {
        this.requiresVerification = requiresVerification;
        this.isPendingVerification = isPendingVerification;
        this.pendingVerificationFailureHandler = pendingVerificationFailureHandler;
        this.loginVerifier = loginVerifier;
        this.warningLogger = warningLogger;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        try {
            if (event.getPacketType() == PacketType.Login.Client.LOGIN_START) {
                handleLoginStart(event);
            } else if (event.getPacketType() == PacketType.Login.Client.ENCRYPTION_RESPONSE) {
                handleEncryptionResponse(event);
            }
        } catch (RuntimeException e) {
            User user = event.getUser();
            String connectionKey = connectionKey(user);
            String username = loginVerifier.getPendingUsername(connectionKey);
            boolean pendingPremiumEnrollment = loginVerifier.isPendingPremiumEnrollment(connectionKey);
            loginVerifier.cleanupPending(connectionKey);
            handlePendingVerificationFailure(username, pendingPremiumEnrollment);
            warningLogger.accept("Unhandled proxy premium verification error for connection " + connectionKey
                + ": " + e.getMessage());
            closeConnection(user, username == null ? connectionKey : username);
        }
    }

    private void handleLoginStart(PacketReceiveEvent event) {
        WrapperLoginClientLoginStart wrapper = new WrapperLoginClientLoginStart(event);
        String username = wrapper.getUsername();
        if (username == null || !VALID_USERNAME.matcher(username).matches()) {
            return;
        }

        String normalizedName = username.toLowerCase(Locale.ROOT);
        if (!requiresVerification.test(normalizedName)) {
            return;
        }
        boolean pendingPremiumEnrollment = isPendingVerification.test(normalizedName);

        User user = event.getUser();
        String connectionKey = connectionKey(user);
        UUID playerUuid = wrapper.getPlayerUUID().orElse(null);

        event.setCancelled(true);
        byte[] verifyToken = loginVerifier.startVerification(
            connectionKey, normalizedName, playerUuid, pendingPremiumEnrollment);
        WrapperLoginServerEncryptionRequest encReq = new WrapperLoginServerEncryptionRequest(
            "", loginVerifier.getPublicKey(), verifyToken, true);
        user.sendPacket(encReq);
    }

    private void handleEncryptionResponse(PacketReceiveEvent event) {
        User user = event.getUser();
        String connectionKey = connectionKey(user);
        if (!loginVerifier.hasPending(connectionKey)) {
            return;
        }

        String username = loginVerifier.getPendingUsername(connectionKey);
        UUID playerUuid = loginVerifier.getPendingPlayerUuid(connectionKey);
        boolean pendingPremiumEnrollment = loginVerifier.isPendingPremiumEnrollment(connectionKey);
        ClientVersion clientVersion = user.getClientVersion();
        WrapperLoginClientEncryptionResponse wrapper = new WrapperLoginClientEncryptionResponse(event);

        byte[] encryptedSharedSecret = wrapper.getEncryptedSharedSecret().clone();
        event.setCancelled(true);

        byte[] sharedSecret;
        try {
            sharedSecret = loginVerifier.decryptData(encryptedSharedSecret);
        } catch (GeneralSecurityException e) {
            warningLogger.accept("Proxy premium RSA decryption failed for '" + username + "': " + e.getMessage());
            loginVerifier.cleanupPending(connectionKey);
            handlePendingVerificationFailure(username, pendingPremiumEnrollment);
            closeConnection(user, username);
            return;
        }

        if (!enableChannelEncryption(user.getChannel(), sharedSecret)) {
            loginVerifier.cleanupPending(connectionKey);
            handlePendingVerificationFailure(username, pendingPremiumEnrollment);
            closeConnection(user, username);
            return;
        }

        Optional<byte[]> encryptedVerifyTokenOpt = wrapper.getEncryptedVerifyToken();
        if (!encryptedVerifyTokenOpt.isPresent()) {
            warningLogger.accept("Proxy premium verification for '" + username
                + "' received a signed nonce instead of a verify token; resuming offline login over the encrypted channel");
            handlePendingVerificationFailure(username, pendingPremiumEnrollment);
            resumeLogin(user, username, clientVersion, playerUuid);
            return;
        }

        byte[] encryptedVerifyToken = encryptedVerifyTokenOpt.get().clone();

        loginVerifier.completeVerification(connectionKey, sharedSecret, encryptedVerifyToken)
            .thenAccept(maybeUuid -> {
                if (maybeUuid.isPresent()) {
                    loginVerifier.storeVerified(username, maybeUuid.get());
                } else {
                    handlePendingVerificationFailure(username, pendingPremiumEnrollment);
                }
                resumeLogin(user, username, clientVersion, playerUuid);
            })
            .exceptionally(ex -> {
                warningLogger.accept("Unexpected proxy premium verification error for '" + username + "': "
                    + ex.getMessage());
                handlePendingVerificationFailure(username, pendingPremiumEnrollment);
                resumeLogin(user, username, clientVersion, playerUuid);
                return null;
            });
    }

    private boolean enableChannelEncryption(Object channel, byte[] sharedSecret) {
        try {
            SecretKeySpec key = new SecretKeySpec(sharedSecret, "AES");
            IvParameterSpec iv = new IvParameterSpec(sharedSecret);

            Cipher decryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            decryptCipher.init(Cipher.DECRYPT_MODE, key, iv);

            Cipher encryptCipher = Cipher.getInstance("AES/CFB8/NoPadding");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key, iv);

            ChannelPipeline pipeline = (ChannelPipeline) ChannelHelper.getPipeline(channel);
            String decoderAnchor = findPipelineAnchor(pipeline, PacketEvents.DECODER_NAME, "splitter");
            String encoderAnchor = findPipelineAnchor(pipeline, PacketEvents.ENCODER_NAME, "prepender");
            if (decoderAnchor == null || encoderAnchor == null) {
                warningLogger.accept("Failed to install proxy AES cipher handlers: missing pipeline anchors in "
                    + ChannelHelper.pipelineHandlerNamesAsString(channel));
                return false;
            }
            if (pipeline.get("authme-premium-decrypt") != null) {
                pipeline.remove("authme-premium-decrypt");
            }
            if (pipeline.get("authme-premium-encrypt") != null) {
                pipeline.remove("authme-premium-encrypt");
            }
            pipeline.addBefore(decoderAnchor, "authme-premium-decrypt", new AesCfb8Decoder(decryptCipher));
            pipeline.addBefore(encoderAnchor, "authme-premium-encrypt", new AesCfb8Encoder(encryptCipher));
            return true;
        } catch (GeneralSecurityException | RuntimeException e) {
            warningLogger.accept("Failed to install proxy AES cipher handlers: " + e.getMessage()
                + " | pipeline=" + ChannelHelper.pipelineHandlerNamesAsString(channel));
            return false;
        }
    }

    private void resumeLogin(User user, String username, ClientVersion clientVersion, UUID playerUuid) {
        ChannelHelper.runInEventLoop(user.getChannel(), () -> {
            WrapperLoginClientLoginStart resumePacket =
                new WrapperLoginClientLoginStart(clientVersion, username, null, playerUuid);
            resumePacket.prepareForSend(user.getChannel(), false);

            if (!resumeLoginInDecoderContext(user, resumePacket)) {
                ReferenceCountUtil.release(resumePacket.getBuffer());
                closeConnection(user, username);
            }
        });
    }

    private void closeConnection(User user, String username) {
        ChannelHelper.runInEventLoop(user.getChannel(), user::closeConnection);
        warningLogger.accept("Closed proxy premium verification connection for '" + username + "'");
    }

    private void handlePendingVerificationFailure(String username, boolean pendingPremiumEnrollment) {
        if (pendingPremiumEnrollment && username != null) {
            pendingVerificationFailureHandler.accept(username.toLowerCase(Locale.ROOT));
        }
    }

    private static String findPipelineAnchor(ChannelPipeline pipeline, String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null && pipeline.get(candidate) != null) {
                return candidate;
            }
        }
        return null;
    }

    private static String connectionKey(User user) {
        InetSocketAddress address = user.getAddress();
        return address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    private boolean resumeLoginInDecoderContext(User user, WrapperLoginClientLoginStart resumePacket) {
        ChannelPipeline pipeline = (ChannelPipeline) ChannelHelper.getPipeline(user.getChannel());
        if (pipeline.get(PacketEvents.DECODER_NAME) == null) {
            warningLogger.accept("Failed to resume proxy premium login for '" + resumePacket.getUsername()
                + "': missing PacketEvents decoder in " + ChannelHelper.pipelineHandlerNamesAsString(user.getChannel()));
            return false;
        }

        ChannelHelper.fireChannelReadInContext(user.getChannel(), PacketEvents.DECODER_NAME, resumePacket.getBuffer());
        return true;
    }
}
