package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.inject.Inject;

public class BungeeReceiver implements PluginMessageListener, SettingsDependent {

    private final AuthMe plugin;
    private final BukkitService bukkitService;
    private final Management management;
    private final DataSource dataSource;

    private boolean isEnabled;

    @Inject
    BungeeReceiver(AuthMe plugin, BukkitService bukkitService, Management management, DataSource dataSource,
                   Settings settings) {
        this.plugin = plugin;
        this.bukkitService = bukkitService;
        this.management = management;
        this.dataSource = dataSource;
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);

        if (this.isEnabled) {
            Messenger messenger = plugin.getServer().getMessenger();
            if (!messenger.isIncomingChannelRegistered(plugin, "BungeeCord")) {
                messenger.registerIncomingPluginChannel(plugin, "BungeeCord", this);
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!isEnabled) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String subchannel = in.readUTF();
        if (!"AuthMe".equals(subchannel)) {
            return;
        }

        String type = in.readUTF();
        String name = in.readUTF();
        switch (type) {
            case MessageType.UNREGISTER:
                dataSource.invalidateCache(name);
                break;
            case MessageType.REFRESH_PASSWORD:
            case MessageType.REFRESH_QUITLOC:
            case MessageType.REFRESH_EMAIL:
            case MessageType.REFRESH:
                dataSource.refreshCache(name);
                break;
            case MessageType.BUNGEE_LOGIN:
                handleBungeeLogin(name);
                break;
            default:
                ConsoleLogger.debug("Received unsupported bungeecord message type! ({0})", type);
        }
    }

    private void handleBungeeLogin(String name) {
        Player player = bukkitService.getPlayerExact(name);
        if (player != null && player.isOnline()) {
            management.forceLogin(player);
            ConsoleLogger.info("The user " + player.getName() + " has been automatically logged in, "
                + "as requested by the AuthMeBungee integration.");
        }

    }

}
