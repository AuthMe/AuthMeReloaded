package fr.xephi.authme.hooks;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.SessionManager;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.inject.Inject;


public class BungeeCordMessage implements PluginMessageListener {

    @Inject
    private DataSource dataSource;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private SessionManager sessionManager;

    @Inject
    private AuthMe plugin;
    
    @Inject
    private NewSetting settings;

    BungeeCordMessage() { }


    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!"BungeeCord".equals(channel)) {
            return;
        }
        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String subChannel = in.readUTF();
        if ("AuthMe".equalsIgnoreCase(subChannel)) {
            String str = in.readUTF();
            final String[] args = str.split(";");
            final String act = args[0];
            final String name = args[1];
            bukkitService.runTaskAsynchronously(new Runnable() {
                @Override
                public void run() {
                    PlayerAuth auth = dataSource.getAuth(name);
                    if (auth == null) {
                        return;
                    }
                    if ("login".equals(act)) {
                        playerCache.updatePlayer(auth);
                        dataSource.setLogged(name);
                        //START 03062016 sgdc3: should fix #731 but we need to recode this mess
                        if (sessionManager.hasSession(name)) {
                            sessionManager.cancelSession(name);
                        }
                        //END

                        if (!settings.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
                            ConsoleLogger.info("Player " + auth.getNickname() + " has logged in from one of your server!");
                        }
                    } else if ("logout".equals(act)) {
                        playerCache.removePlayer(name);
                        dataSource.setUnlogged(name);
                        if (!settings.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
                            ConsoleLogger.info("Player " + auth.getNickname() + " has logged out from one of your server!");
                        }
                    } else if ("register".equals(act)) {
                        if (!settings.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
                            ConsoleLogger.info("Player " + auth.getNickname() + " has registered out from one of your server!");
                        }
                    } else if ("changepassword".equals(act)) {
                        final String password = args[2];
                        final String salt = args.length >= 4 ? args[3] : null;
                        auth.setPassword(new HashedPassword(password, salt));
                        playerCache.updatePlayer(auth);
                        dataSource.updatePassword(auth);
                    }

                }
            });
        }
    }

}
