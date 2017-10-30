package fr.xephi.authme.service;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.datasource.CacheDataSource;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.Messenger;
import org.bukkit.plugin.messaging.PluginMessageListener;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * Class to manage all BungeeCord related processes.
 */
public class BungeeService implements SettingsDependent, PluginMessageListener {

    private AuthMe plugin;
    private BukkitService service;

    private boolean isEnabled;
    private String destinationServerOnLogin;

    private DataSource dataSource;

    /*
     * Constructor.
     */
    @Inject
    BungeeService(AuthMe plugin, BukkitService service, Settings settings, DataSource dataSource) {
        this.plugin = plugin;
        this.service = service;
        this.dataSource = dataSource;
        reload(settings);
    }

    @Override
    public void reload(Settings settings) {
        this.isEnabled = settings.getProperty(HooksSettings.BUNGEECORD);
        this.destinationServerOnLogin = settings.getProperty(HooksSettings.BUNGEECORD_SERVER);
        Messenger messenger = plugin.getServer().getMessenger();
        if (!this.isEnabled) {
            return;
        }
        if (!messenger.isOutgoingChannelRegistered(plugin, "BungeeCord")) {
            messenger.registerOutgoingPluginChannel(plugin, "BungeeCord");
        }
        if (!messenger.isIncomingChannelRegistered(plugin, "BungeeCord")) {
            messenger.registerIncomingPluginChannel(plugin, "BungeeCord", this);
        }
    }

    private void sendBungeecordMessage(String... data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        for(String element : data) {
            out.writeUTF(element);
        }
        service.sendPluginMessage("BungeeCord", out.toByteArray());
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

        service.scheduleSyncDelayedTask(() ->
            sendBungeecordMessage("Connect", player.getName(), destinationServerOnLogin), 20L);
    }

    private void sendAuthMeBungeecordMessage(String type, String... data) {
        if(!isEnabled) {
            return;
        }

        List<String> dataList = Arrays.asList(data);
        dataList.add(0, "AuthMe");
        dataList.add(1, type);
        sendBungeecordMessage(dataList.toArray(new String[dataList.size()]));
    }

    public void sendLogin(String name) {
        sendAuthMeBungeecordMessage(AuthMeBungeeMessageType.LOGIN, name.toLowerCase());
    }

    public void sendLogout(String name) {
        sendAuthMeBungeecordMessage(AuthMeBungeeMessageType.LOGOUT, name.toLowerCase());
    }

    public void sendRegister(String name) {
        sendAuthMeBungeecordMessage(AuthMeBungeeMessageType.REGISTER, name.toLowerCase());
    }

    public void sendUnregister(String name) {
        sendAuthMeBungeecordMessage(AuthMeBungeeMessageType.UNREGISTER, name.toLowerCase());
    }

    public void sendRefreshPassword(String name) {
        sendAuthMeBungeecordMessage(AuthMeBungeeMessageType.REFRESH_PASSWORD, name.toLowerCase());
    }

    public void sendRefreshSession(String name) {
        sendAuthMeBungeecordMessage(AuthMeBungeeMessageType.REFRESH_SESSION, name.toLowerCase());
    }

    public void sendRefreshQuitLoc(String name) {
        sendAuthMeBungeecordMessage(AuthMeBungeeMessageType.REFRESH_QUITLOC, name.toLowerCase());
    }

    public void sendRefreshEmail(String name) {
        sendAuthMeBungeecordMessage(AuthMeBungeeMessageType.REFRESH_EMAIL, name.toLowerCase());
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] data) {
        if(!isEnabled) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String subchannel = in.readUTF();
        if(!"Authme".equals(subchannel)) {
            return;
        }

        String type = in.readUTF();
        switch (type) {
            case AuthMeBungeeMessageType.UNREGISTER:
                handleRemove(in.readUTF());
                break;
            case AuthMeBungeeMessageType.REFRESH_PASSWORD:
            case AuthMeBungeeMessageType.REFRESH_QUITLOC:
            case AuthMeBungeeMessageType.REFRESH_EMAIL:
            case AuthMeBungeeMessageType.REFRESH:
                handleRefresh(in.readUTF());
                break;
            default:
                ConsoleLogger.debug("Received unsupported bungeecord message type! (" + type + ")");
        }
    }

    private void handleRefresh(String name) {
        if(!(dataSource instanceof CacheDataSource)) {
            return;
        }
        CacheDataSource cacheDataSource = (CacheDataSource) dataSource;

        if (cacheDataSource.getCachedAuths().getIfPresent(name) == null) {
            return;
        }
        cacheDataSource.getCachedAuths().refresh(name);
    }

    private void handleRemove(String name) {
        if(!(dataSource instanceof CacheDataSource)) {
            return;
        }
        CacheDataSource cacheDataSource = (CacheDataSource) dataSource;

        cacheDataSource.getCachedAuths().invalidate(name);
    }

    public class AuthMeBungeeMessageType {
        public static final String LOGIN = "login";
        public static final String LOGOUT = "logout";
        public static final String REGISTER = "register";
        public static final String UNREGISTER = "unregister";
        public static final String REFRESH_PASSWORD = "refresh.password";
        public static final String REFRESH_SESSION = "refresh.session";
        public static final String REFRESH_QUITLOC = "refresh.quitloc";
        public static final String REFRESH_EMAIL = "refresh.email";
        public static final String REFRESH = "refresh";

        private AuthMeBungeeMessageType() {
        }
    }

}
