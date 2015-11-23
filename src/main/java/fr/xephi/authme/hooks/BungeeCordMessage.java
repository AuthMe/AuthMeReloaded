package fr.xephi.authme.hooks;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 */
public class BungeeCordMessage implements PluginMessageListener {

    public AuthMe plugin;

    /**
     * Constructor for BungeeCordMessage.
     *
     * @param plugin AuthMe
     */
    public BungeeCordMessage(AuthMe plugin) {
        this.plugin = plugin;
    }

    /**
     * Method onPluginMessageReceived.
     *
     * @param channel String
     * @param player  Player
     * @param message byte[]
     * @see org.bukkit.plugin.messaging.PluginMessageListener#onPluginMessageReceived(String, Player, byte[])
     */
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
