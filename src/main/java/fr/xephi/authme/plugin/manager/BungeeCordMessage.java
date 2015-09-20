package fr.xephi.authme.plugin.manager;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

public class BungeeCordMessage implements PluginMessageListener {

    public AuthMe plugin;

    public BungeeCordMessage(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player,
                                        byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if (subChannel.equals("IP")) { // We need only the IP channel
            String ip = in.readUTF();
            // Put the IP (only the ip not the port) in the hashMap
            plugin.realIp.put(player.getName().toLowerCase(), ip);
        }
    }

}
