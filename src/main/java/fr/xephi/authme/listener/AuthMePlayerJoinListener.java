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
import fr.xephi.authme.util.StringUtils;
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
    @Inject
    private LimboCache limboCache;

    private Pattern nicknamePattern;

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player != null) {
            // Schedule login task so works after the prelogin
            // (Fix found by Koolaid5000)
            bukkitService.runTask(new Runnable() {
                @Override
                public void run() {
                    management.performJoin(player);
                }
            });
        }
    }

    // Note ljacqu 20160528: AsyncPlayerPreLoginEvent is not fired by all servers in offline mode
    // e.g. CraftBukkit does not. So we need to run crucial things in onPlayerLogin, too
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        final String name = event.getName().toLowerCase();
        final boolean isAuthAvailable = dataSource.isAuthAvailable(event.getName());

        try {
            // Potential performance improvement: make checkAntiBot not require `isAuthAvailable` info and use
            // "checkKickNonRegistered" as last -> no need to query the DB before checking antibot / name
            checkAntibot(name, isAuthAvailable);
            checkKickNonRegistered(isAuthAvailable);
            checkIsValidName(name);
        } catch (VerificationFailedException e) {
            event.setKickMessage(m.retrieveSingle(e.getReason(), e.getArgs()));
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        if (player == null || Utils.isUnrestricted(player)) {
            return;
        } else if (refusePlayerForFullServer(event)) {
            return;
        } else if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }

        final String name = player.getName().toLowerCase();
        final PlayerAuth auth = dataSource.getAuth(player.getName());
        final boolean isAuthAvailable = auth != null;

        try {
            checkAntibot(name, isAuthAvailable);
            checkKickNonRegistered(isAuthAvailable);
            checkIsValidName(name);
            checkNameCasing(player, auth);
            checkSingleSession(player);
            checkPlayerCountry(isAuthAvailable, event);
        } catch (VerificationFailedException e) {
            event.setKickMessage(m.retrieveSingle(e.getReason(), e.getArgs()));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        antiBot.handlePlayerJoin(player);

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

    /**
     * Selects a non-VIP player to kick when a VIP player joins the server when full.
     *
     * @param onlinePlayers list of online players
     * @return the player to kick, or null if none applicable
     */
    private Player generateKickPlayer(Collection<? extends Player> onlinePlayers) {
        for (Player player : onlinePlayers) {
            if (!permissionsManager.hasPermission(player, PlayerStatePermission.IS_VIP)) {
                return player;
            }
        }
        return null;
    }

    /**
     * Checks if Antibot is enabled.
     *
     * @param playerName the name of the player (lowercase)
     * @param isAuthAvailable whether or not the player is registered
     */
    private void checkAntibot(String playerName, boolean isAuthAvailable) throws VerificationFailedException {
        if (antiBot.getAntiBotStatus() == AntiBot.AntiBotStatus.ACTIVE && !isAuthAvailable) {
            antiBot.antibotKicked.addIfAbsent(playerName);
            throw new VerificationFailedException(MessageKey.KICK_ANTIBOT);
        }
    }

    /**
     * Checks whether non-registered players should be kicked, and if so, whether the player should be kicked.
     *
     * @param isAuthAvailable whether or not the player is registered
     */
    private void checkKickNonRegistered(boolean isAuthAvailable) throws VerificationFailedException {
        if (!isAuthAvailable && settings.getProperty(RestrictionSettings.KICK_NON_REGISTERED)) {
            throw new VerificationFailedException(MessageKey.MUST_REGISTER_MESSAGE);
        }
    }

    /**
     * Checks that the name adheres to the configured username restrictions.
     *
     * @param name the name to verify
     */
    private void checkIsValidName(String name) throws VerificationFailedException {
        if (name.length() > settings.getProperty(RestrictionSettings.MAX_NICKNAME_LENGTH)
            || name.length() < settings.getProperty(RestrictionSettings.MIN_NICKNAME_LENGTH)) {
            throw new VerificationFailedException(MessageKey.INVALID_NAME_LENGTH);
        }
        if (!nicknamePattern.matcher(name).matches()) {
            throw new VerificationFailedException(MessageKey.INVALID_NAME_CHARACTERS, nicknamePattern.pattern());
        }
    }

    /**
     * Handles the case of a full server and verifies if the user's connection should really be refused
     * by adjusting the event object accordingly. Attempts to kick a non-VIP player to make room if the
     * joining player is a VIP.
     *
     * @param event the login event to verify
     * @return true if the player's connection should be refused (i.e. the event does not need to be processed
     * further), false if the player is not refused
     */
    private boolean refusePlayerForFullServer(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        if (event.getResult() != PlayerLoginEvent.Result.KICK_FULL) {
            // Server is not full, no need to do anything
            return false;
        } else if (!permissionsManager.hasPermission(player, PlayerStatePermission.IS_VIP)) {
            // Server is full and player is NOT VIP; set kick message and proceed with kick
            event.setKickMessage(m.retrieveSingle(MessageKey.KICK_FULL_SERVER));
            return true;
        }

        // Server is full and player is VIP; attempt to kick a non-VIP player to make room
        Collection<? extends Player> onlinePlayers = bukkitService.getOnlinePlayers();
        if (onlinePlayers.size() < plugin.getServer().getMaxPlayers()) {
            event.allow();
            return false;
        }
        Player nonVipPlayer = generateKickPlayer(onlinePlayers);
        if (nonVipPlayer != null) {
            nonVipPlayer.kickPlayer(m.retrieveSingle(MessageKey.KICK_FOR_VIP));
            event.allow();
            return false;
        } else {
            ConsoleLogger.info("VIP player " + player.getName() + " tried to join, but the server was full");
            event.setKickMessage(m.retrieveSingle(MessageKey.KICK_FULL_SERVER));
            return true;
        }
    }

    /**
     * Checks that the casing in the username corresponds to the one in the database, if so configured.
     *
     * @param player the player to verify
     * @param auth the auth object associated with the player
     */
    private void checkNameCasing(Player player, PlayerAuth auth) throws VerificationFailedException {
        if (auth != null && settings.getProperty(RegistrationSettings.PREVENT_OTHER_CASE)) {
            String realName = auth.getRealName(); // might be null or "Player"
            String connectingName = player.getName();

            if (StringUtils.isEmpty(realName) || "Player".equals(realName)) {
                dataSource.updateRealName(connectingName.toLowerCase(), connectingName);
            } else if (!realName.equals(connectingName)) {
                throw new VerificationFailedException(MessageKey.INVALID_NAME_CASE, realName, connectingName);
            }
        }
    }

    /**
     * Checks that the player's country is admitted if he is not registered.
     *
     * @param isAuthAvailable whether or not the user is registered
     * @param event the login event of the player
     */
    private void checkPlayerCountry(boolean isAuthAvailable,
                                    PlayerLoginEvent event) throws VerificationFailedException {
        if (!isAuthAvailable && settings.getProperty(ProtectionSettings.ENABLE_PROTECTION)) {
            String playerIp = event.getAddress().getHostAddress();
            if (!validationService.isCountryAdmitted(playerIp)) {
                throw new VerificationFailedException(MessageKey.COUNTRY_BANNED_ERROR);
            }
        }
    }

    /**
     * Checks if a player with the same name (case-insensitive) is already playing and refuses the
     * connection if so configured.
     *
     * @param player the player to verify
     */
    private void checkSingleSession(Player player) throws VerificationFailedException {
        if (!settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)) {
            return;
        }

        Player onlinePlayer = bukkitService.getPlayerExact(player.getName());
        if (onlinePlayer != null) {
            String name = player.getName().toLowerCase();
            LimboPlayer limbo = limboCache.getLimboPlayer(name);
            if (limbo != null && PlayerCache.getInstance().isAuthenticated(name)) {
                Utils.addNormal(player, limbo.getGroup());
                limboCache.deleteLimboPlayer(name);
            }
            throw new VerificationFailedException(MessageKey.USERNAME_ALREADY_ONLINE_ERROR);
        }
    }

    /**
     * Exception thrown when a verification has failed and the player should be kicked.
     */
    private static final class VerificationFailedException extends Exception {
        private final MessageKey reason;
        private final String[] args;

        public VerificationFailedException(MessageKey reason, String... args) {
            this.reason = reason;
            this.args = args;
        }

        public MessageKey getReason() {
            return reason;
        }

        public String[] getArgs() {
            return args;
        }
    }
}
