package fr.xephi.authme.service.bungeecord;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.inject.Inject;

public class BungeeReceiver implements PluginMessageListener {

    @Inject
    private BukkitService service;

    @Inject
    private Management management;

    @Inject
    private DataSource dataSource;

    @Inject
    private BungeeService bungeeService;

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if (!bungeeService.isEnabled()) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String subchannel = in.readUTF();
        if (!"Authme".equals(subchannel)) {
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
        Player player = service.getPlayerExact(name);
        if (player == null || !player.isOnline()) {
            return;
        }
        management.forceLogin(player);
        ConsoleLogger.info("The user " + player.getName() + " has been automatically logged in, "
            + "as requested by the AuthMeBungee integration.");
    }

}
