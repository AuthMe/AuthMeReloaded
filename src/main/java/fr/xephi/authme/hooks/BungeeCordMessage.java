package fr.xephi.authme.hooks;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.crypts.HashedPassword;
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
            final String[] args = str.split(";");
            final String act = args[0];
            final String name = args[1];
            final DataSource dataSource = plugin.getDataSource();
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
                @Override
                public void run() {
                    PlayerAuth auth = dataSource.getAuth(name);
                    if (auth == null) {
                        return;
                    }
                    if ("login".equals(act)) {
                        PlayerCache.getInstance().updatePlayer(auth);
                        dataSource.setLogged(name);
                        ConsoleLogger.info("Player " + auth.getNickname()
                            + " has logged in from one of your server!");
                    } else if ("logout".equals(act)) {
                        PlayerCache.getInstance().removePlayer(name);
                        dataSource.setUnlogged(name);
                        ConsoleLogger.info("Player " + auth.getNickname()
                            + " has logged out from one of your server!");
                    } else if ("register".equals(act)) {
                        ConsoleLogger.info("Player " + auth.getNickname()
                            + " has registered out from one of your server!");
                    } else if ("changepassword".equals(act)) {
                    	final String password = args[2];
                        final String salt = args.length >= 4 ? args[3] : null;
                    	auth.setPassword(new HashedPassword(password, salt));
                    	PlayerCache.getInstance().updatePlayer(auth);
                        dataSource.updatePassword(auth);
                    }

                }
            });
        }
    }

}
