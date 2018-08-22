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
import java.util.Optional;

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

        Optional<MessageType> type = MessageType.fromId(in.readUTF());
        if(!type.isPresent()) {
            ConsoleLogger.debug("Received unsupported bungeecord message type! ({0})", type);
            return;
        }

        String argument = in.readUTF();
        switch (type.get()) {
            case UNREGISTER:
                dataSource.invalidateCache(argument);
                break;
            case REFRESH_PASSWORD:
            case REFRESH_QUITLOC:
            case REFRESH_EMAIL:
            case REFRESH:
                dataSource.refreshCache(argument);
                break;
            case PERFORM_LOGIN:
                performLogin(argument);
                break;
            default:
        }
    }

    private void performLogin(String name) {
        Player player = bukkitService.getPlayerExact(name);
        if (player != null && player.isOnline()) {
            management.forceLogin(player);
            ConsoleLogger.info("The user " + player.getName() + " has been automatically logged in, "
                + "as requested via plugin messaging.");
        }

    }

}
