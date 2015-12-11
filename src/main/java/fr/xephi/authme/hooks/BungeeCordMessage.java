package fr.xephi.authme.hooks;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.CacheDataSource;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

/**
 */
public class BungeeCordMessage implements PluginMessageListener {

    public final AuthMe plugin;

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
     *
     * @see org.bukkit.plugin.messaging.PluginMessageListener#onPluginMessageReceived(String, Player, byte[])
     */
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
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
        if (subChannel.equalsIgnoreCase("AuthMe")) {
            String str = in.readUTF();
            String[] args = str.split(";");
            String name = args[1];
            PlayerAuth auth = plugin.database.getAuth(name);
            if (auth == null) {
                return;
            }
            if ("login".equals(args[0])) {
                PlayerCache.getInstance().updatePlayer(auth);
                plugin.database.setLogged(name);
                ConsoleLogger.info("Player " + auth.getNickname() + " has logged in from one of your server!");
            } else if ("logout".equals(args[0])) {
                PlayerCache.getInstance().removePlayer(name);
                plugin.database.setUnlogged(name);
                ConsoleLogger.info("Player " + auth.getNickname() + " has logged out from one of your server!");
            } else if ("register".equals(args[0])) {
                if (plugin.database instanceof CacheDataSource) {
                    ((CacheDataSource)plugin.database).addAuthToCache(auth);
                }
                ConsoleLogger.info("Player " + auth.getNickname() + " has registered out from one of your server!");
            }
        }
    }

}
