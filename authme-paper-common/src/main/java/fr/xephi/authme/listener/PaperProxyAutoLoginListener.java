package fr.xephi.authme.listener;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.service.PreJoinDialogService;
import fr.xephi.authme.service.bungeecord.BungeeReceiver;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import io.papermc.paper.connection.PlayerConnection;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.inject.Inject;

/**
 * Receives proxy {@code perform.login} messages that arrive during Paper/Folia's connection
 * configuration phase — before the connecting player exists as a {@link Player} — and uses them to
 * dismiss the blocking pre-join login dialog. This lets an already-authenticated player who switches
 * back to an auth server be auto-logged in instead of being prompted to log in again.
 *
 * <p>The standard {@link BungeeReceiver} only handles play-phase messages (its listener callback
 * requires a {@link Player}). This listener implements the configuration-phase overload of
 * {@code PluginMessageListener.onPluginMessageReceived(String, PlayerConnection, byte[])}, which is
 * the only point at which the proxy signal can reach the backend before the dialog decision is made
 * (the post-join {@code perform.login} arrives only after the configuration phase completes).
 */
public class PaperProxyAutoLoginListener implements Listener, PluginMessageListener, SettingsDependent {

    private static final String AUTHME_CHANNEL = "authme:main";

    private final AuthMe plugin;
    private final BungeeReceiver bungeeReceiver;
    private final PreJoinDialogService preJoinDialogService;

    private boolean enabled;
    private boolean channelRegistered;

    @Inject
    PaperProxyAutoLoginListener(AuthMe plugin, BungeeReceiver bungeeReceiver,
                                PreJoinDialogService preJoinDialogService, Settings settings) {
        this.plugin = plugin;
        this.bungeeReceiver = bungeeReceiver;
        this.preJoinDialogService = preJoinDialogService;
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
        this.enabled = settings.getProperty(HooksSettings.BUNGEECORD);
        Messenger messenger = plugin.getServer().getMessenger();
        if (messenger == null) {
            return;
        }
        if (enabled && !channelRegistered) {
            messenger.registerIncomingPluginChannel(plugin, AUTHME_CHANNEL, this);
            channelRegistered = true;
        } else if (!enabled && channelRegistered) {
            messenger.unregisterIncomingPluginChannel(plugin, AUTHME_CHANNEL, this);
            channelRegistered = false;
        }
    }

    /**
     * Play-phase messages are handled by {@link BungeeReceiver}; nothing to do here.
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        // no-op
    }

    /**
     * Handles AuthMe messages that arrive while the player is still in the configuration phase.
     */
    @Override
    public void onPluginMessageReceived(String channel, PlayerConnection connection, byte[] message) {
        if (!enabled || !AUTHME_CHANNEL.equals(channel)
            || !(connection instanceof PlayerConfigurationConnection)) {
            return;
        }
        String name = bungeeReceiver.handleConfigPhasePerformLogin(message);
        if (name != null) {
            // If the player is already blocked in the pre-join dialog, dismiss it (force-login on join).
            // If the dialog has not been shown yet, the queued proxy session makes shouldSkipDialogs()
            // skip it; PaperDialogFlowListener also re-checks that queue right after registering.
            preJoinDialogService.approvePreJoinForceLogin(name);
        }
    }
}
