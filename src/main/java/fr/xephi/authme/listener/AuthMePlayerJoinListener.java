package fr.xephi.authme.listener;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AntiBot;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Listener for player join events.
 */
public class AuthMePlayerJoinListener implements Listener, Reloadable {

    @Inject
    private BukkitService bukkitService;
    @Inject
    private DataSource dataSource;
    @Inject
    private AntiBot antiBot;
    @Inject
    private Management management;
    @Inject
    private NewSetting settings;
    @Inject
    private Messages m;
    @Inject
    private PermissionsManager permissionsManager;
    @Inject
    private ValidationService validationService;
    @Inject
    private AuthMe plugin;

    private Pattern nicknamePattern;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        // Schedule login task so works after the prelogin
        // (Fix found by Koolaid5000)
        bukkitService.runTask(new Runnable() {
            @Override
            public void run() {
                management.performJoin(player);
            }
        });
    }

    // Note ljacqu 20160528: AsyncPlayerPreLoginEvent is not fired by all servers in offline mode
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerAuth auth = dataSource.getAuth(event.getName());
        if (auth == null && antiBot.getAntiBotStatus() == AntiBot.AntiBotStatus.ACTIVE) {
            event.setKickMessage(m.retrieveSingle(MessageKey.KICK_ANTIBOT));
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            antiBot.antibotKicked.addIfAbsent(event.getName());
            return;
        }
        if (auth == null && settings.getProperty(RestrictionSettings.KICK_NON_REGISTERED)) {
            event.setKickMessage(m.retrieveSingle(MessageKey.MUST_REGISTER_MESSAGE));
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            return;
        }
        final String name = event.getName().toLowerCase();
        if (name.length() > settings.getProperty(RestrictionSettings.MAX_NICKNAME_LENGTH) || name.length() < settings.getProperty(RestrictionSettings.MIN_NICKNAME_LENGTH)) {
            event.setKickMessage(m.retrieveSingle(MessageKey.INVALID_NAME_LENGTH));
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            return;
        }
        if (settings.getProperty(RegistrationSettings.PREVENT_OTHER_CASE) && auth != null && auth.getRealName() != null) {
            String realName = auth.getRealName();
            if (!realName.isEmpty() && !"Player".equals(realName) && !realName.equals(event.getName())) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(m.retrieveSingle(MessageKey.INVALID_NAME_CASE, realName, event.getName()));
                return;
            }
            if (realName.isEmpty() || "Player".equals(realName)) {
                dataSource.updateRealName(event.getName().toLowerCase(), event.getName());
            }
        }

        if (auth == null && settings.getProperty(ProtectionSettings.ENABLE_PROTECTION)) {
            String playerIp = event.getAddress().getHostAddress();
            if (!validationService.isCountryAdmitted(playerIp)) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(m.retrieveSingle(MessageKey.COUNTRY_BANNED_ERROR));
                return;
            }
        }

        final Player player = bukkitService.getPlayerExact(name);
        // Check if forceSingleSession is set to true, so kick player that has
        // joined with same nick of online player
        if (player != null && settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(m.retrieveSingle(MessageKey.USERNAME_ALREADY_ONLINE_ERROR));
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (limbo != null && PlayerCache.getInstance().isAuthenticated(name)) {
                Utils.addNormal(player, limbo.getGroup());
                LimboCache.getInstance().deleteLimboPlayer(name);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        if (player == null || Utils.isUnrestricted(player)) {
            return;
        }

        if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL) {
            if (permissionsManager.hasPermission(player, PlayerStatePermission.IS_VIP)) {
                int playersOnline = bukkitService.getOnlinePlayers().size();
                if (playersOnline > plugin.getServer().getMaxPlayers()) {
                    event.allow();
                } else {
                    Player pl = generateKickPlayer(bukkitService.getOnlinePlayers());
                    if (pl != null) {
                        pl.kickPlayer(m.retrieveSingle(MessageKey.KICK_FOR_VIP));
                        event.allow();
                    } else {
                        ConsoleLogger.info("The player " + event.getPlayer().getName() + " tried to join, but the server was full");
                        event.setKickMessage(m.retrieveSingle(MessageKey.KICK_FULL_SERVER));
                        event.setResult(PlayerLoginEvent.Result.KICK_FULL);
                    }
                }
            } else {
                event.setKickMessage(m.retrieveSingle(MessageKey.KICK_FULL_SERVER));
                event.setResult(PlayerLoginEvent.Result.KICK_FULL);
                return;
            }
        }

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }

        final String name = player.getName().toLowerCase();
        boolean isAuthAvailable = dataSource.isAuthAvailable(name);

        if (antiBot.getAntiBotStatus() == AntiBot.AntiBotStatus.ACTIVE && !isAuthAvailable) {
            event.setKickMessage(m.retrieveSingle(MessageKey.KICK_ANTIBOT));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            antiBot.antibotKicked.addIfAbsent(player.getName());
            return;
        }

        if (settings.getProperty(RestrictionSettings.KICK_NON_REGISTERED) && !isAuthAvailable) {
            event.setKickMessage(m.retrieveSingle(MessageKey.MUST_REGISTER_MESSAGE));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        if (name.length() > settings.getProperty(RestrictionSettings.MAX_NICKNAME_LENGTH) || name.length() < settings.getProperty(RestrictionSettings.MIN_NICKNAME_LENGTH)) {
            event.setKickMessage(m.retrieveSingle(MessageKey.INVALID_NAME_LENGTH));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        if (name.equalsIgnoreCase("Player") || !nicknamePattern.matcher(player.getName()).matches()) {
            event.setKickMessage(m.retrieveSingle(MessageKey.INVALID_NAME_CHARACTERS)
                .replace("REG_EX", nicknamePattern.pattern()));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        antiBot.checkAntiBot(player);

        if (settings.getProperty(HooksSettings.BUNGEECORD)) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("IP");
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }

    @PostConstruct
    @Override
    public void reload() {
        String nickRegEx = settings.getProperty(RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS);
        try {
            nicknamePattern = Pattern.compile(nickRegEx);
        } catch (Exception e) {
            nicknamePattern = Pattern.compile(".*?");
            ConsoleLogger.showError("Nickname pattern is not a valid regular expression! "
                + "Fallback to allowing all nicknames");
        }
    }

    // Select the player to kick when a vip player joins the server when full
    private Player generateKickPlayer(Collection<? extends Player> collection) {
        for (Player player : collection) {
            if (!permissionsManager.hasPermission(player, PlayerStatePermission.IS_VIP)) {
                return player;
            }
        }
        return null;
    }
}
