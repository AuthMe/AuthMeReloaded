package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;

import java.util.List;
import java.util.Optional;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;

import javax.inject.Inject;

public class BungeeSender implements SettingsDependent {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(BungeeSender.class);
    private final AuthMe plugin;
    private final BukkitService bukkitService;

    private boolean isEnabled;
    private String destinationServerOnLogin;

    /*
     * Constructor.
     */
    @Inject
    BungeeSender(AuthMe plugin, BukkitService bukkitService, Settings settings) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        this.destinationServerOnLogin = settings.getProperty(HooksSettings.BUNGEECORD_SERVER);
        Messenger messenger = plugin.getServer().getMessenger();

        if (this.isEnabled && messenger != null) {
            if (!messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
                messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
            }
            if (!messenger.isOutgoingChannelRegistered(plugin, "authme:main")) {
                messenger.registerOutgoingPluginChannel(plugin, "authme:main");
            }
        } else if (messenger != null) {
            if (messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
                messenger.unregisterOutgoingPluginChannel(plugin, "BungeeCord");
            }
            if (messenger.isOutgoingChannelRegistered(plugin, "authme:main")) {
                messenger.unregisterOutgoingPluginChannel(plugin, "authme:main");
            }
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Send a player to a specified server. If no server is configured, this will
     * do nothing.
     *
     * @param player The player to send.
     */
    public void connectPlayerOnLogin(Player player) {
        if (!isEnabled || destinationServerOnLogin.isEmpty()) {
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(destinationServerOnLogin);
        byte[] payload = out.toByteArray();
        // Add a small delay, just in case...
        bukkitService.scheduleSyncDelayedTask(player, () ->
            bukkitService.sendBungeeMessage(player, payload), 20L);
    }

    /**
     * Sends a message to the AuthMe plugin messaging channel, if enabled.
     *
     * @param player     The player related to the message
     * @param type       The message type, See {@link MessageType}
     */
    public void sendAuthMeBungeecordMessage(Player player, MessageType type) {
        if (!isEnabled) {
            return;
        }
        if (!plugin.isEnabled()) {
            logger.debug("Tried to send a " + type + " bungeecord message but the plugin was disabled!");
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(type.getId());
        out.writeUTF(player.getName().toLowerCase(java.util.Locale.ROOT));
        bukkitService.sendAuthMePluginMessage(player, out.toByteArray());
    }

    /**
     * Notifies the proxy that the given username has been enrolled as premium.
     * Uses any online player as the message carrier.
     *
     * @param username the player name (will be lowercased)
     */
    public void sendPremiumSet(String username) {
        sendPremiumNotification(MessageType.PREMIUM_SET, username);
    }

    /**
     * Notifies the proxy that a pending premium verification has been started for the given
     * username. The proxy should force Mojang authentication for this player on their next
     * connection but must NOT auto-login them via {@code PERFORM_LOGIN} (that path is reserved
     * for confirmed premium players). Uses any online player as the message carrier.
     *
     * @param username the player name (will be lowercased)
     */
    public void sendPremiumPendingSet(String username) {
        sendPremiumNotification(MessageType.PREMIUM_PENDING_SET, username);
    }

    /**
     * Notifies the proxy that premium mode has been disabled for the given username.
     * Uses any online player as the message carrier.
     *
     * @param username the player name (will be lowercased)
     */
    public void sendPremiumUnset(String username) {
        sendPremiumNotification(MessageType.PREMIUM_UNSET, username);
    }

    /**
     * Sends the full list of premium usernames to the proxy using the given carrier player.
     * Should be called when the proxy starts up (after receiving {@code proxy.started}).
     *
     * @param carrier         the player used as message carrier
     * @param premiumUsernames the list of premium usernames (lowercase)
     */
    public void sendPremiumList(Player carrier, List<String> premiumUsernames) {
        if (!isEnabled || !plugin.isEnabled()) {
            return;
        }
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(MessageType.PREMIUM_LIST.getId());
        out.writeUTF(String.join(",", premiumUsernames));
        bukkitService.sendAuthMePluginMessage(carrier, out.toByteArray());
    }

    private void sendPremiumNotification(MessageType type, String username) {
        if (!isEnabled || !plugin.isEnabled()) {
            return;
        }
        Optional<? extends Player> carrier = bukkitService.getOnlinePlayers().stream().findFirst();
        if (!carrier.isPresent()) {
            logger.warning("Cannot send premium notification to proxy: no online player available as carrier."
                + " Premium state may be stale on the proxy until the next full resync.");
            return;
        }
        Player p = carrier.get();
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(type.getId());
        out.writeUTF(username.toLowerCase(java.util.Locale.ROOT));
        byte[] payload = out.toByteArray();
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() ->
            bukkitService.sendAuthMePluginMessage(p, payload));
    }

}
