package fr.xephi.authme.listener;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.service.AntiBotService;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.StringUtils;
import fr.xephi.authme.util.Utils;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerLoginEvent;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * Service for performing various verifications when a player joins.
 */
public class OnJoinVerifier implements Reloadable {

    @Inject
    private Settings settings;
    @Inject
    private DataSource dataSource;
    @Inject
    private Messages messages;
    @Inject
    private PermissionsManager permissionsManager;
    @Inject
    private AntiBotService antiBotService;
    @Inject
    private ValidationService validationService;
    @Inject
    private BukkitService bukkitService;
    @Inject
    private Server server;

    private Pattern nicknamePattern;

    OnJoinVerifier() {
    }


    @PostConstruct
    @Override
    public void reload() {
        String nickRegEx = settings.getProperty(RestrictionSettings.ALLOWED_NICKNAME_CHARACTERS);
        nicknamePattern = Utils.safePatternCompile(nickRegEx);
    }

    /**
     * Checks if Antibot is enabled.
     *
     * @param joiningPlayer   the joining player to check
     * @param isAuthAvailable whether or not the player is registered
     * @throws FailedVerificationException if the verification fails
     */
    public void checkAntibot(JoiningPlayer joiningPlayer, boolean isAuthAvailable) throws FailedVerificationException {
        if (isAuthAvailable || permissionsManager.hasPermission(joiningPlayer, PlayerStatePermission.BYPASS_ANTIBOT)) {
            return;
        }
        if (antiBotService.shouldKick()) {
            antiBotService.addPlayerKick(joiningPlayer.getName());
            throw new FailedVerificationException(MessageKey.KICK_ANTIBOT);
        }
    }

    /**
     * Checks whether non-registered players should be kicked, and if so, whether the player should be kicked.
     *
     * @param isAuthAvailable whether or not the player is registered
     * @throws FailedVerificationException if the verification fails
     */
    public void checkKickNonRegistered(boolean isAuthAvailable) throws FailedVerificationException {
        if (!isAuthAvailable && settings.getProperty(RestrictionSettings.KICK_NON_REGISTERED)) {
            throw new FailedVerificationException(MessageKey.MUST_REGISTER_MESSAGE);
        }
    }

    /**
     * Checks that the name adheres to the configured username restrictions.
     *
     * @param name the name to verify
     * @throws FailedVerificationException if the verification fails
     */
    public void checkIsValidName(String name) throws FailedVerificationException {
        if (name.length() > settings.getProperty(RestrictionSettings.MAX_NICKNAME_LENGTH)
            || name.length() < settings.getProperty(RestrictionSettings.MIN_NICKNAME_LENGTH)) {
            throw new FailedVerificationException(MessageKey.INVALID_NAME_LENGTH);
        }
        if (!nicknamePattern.matcher(name).matches()) {
            throw new FailedVerificationException(MessageKey.INVALID_NAME_CHARACTERS, nicknamePattern.pattern());
        }
    }

    /**
     * Handles the case of a full server and verifies if the user's connection should really be refused
     * by adjusting the event object accordingly. Attempts to kick a non-VIP player to make room if the
     * joining player is a VIP.
     *
     * @param event the login event to verify
     *
     * @return true if the player's connection should be refused (i.e. the event does not need to be processed
     *         further), false if the player is not refused
     */
    public boolean refusePlayerForFullServer(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        if (event.getResult() != PlayerLoginEvent.Result.KICK_FULL) {
            // Server is not full, no need to do anything
            return false;
        } else if (!permissionsManager.hasPermission(player, PlayerStatePermission.IS_VIP)) {
            // Server is full and player is NOT VIP; set kick message and proceed with kick
            event.setKickMessage(messages.retrieveSingle(player, MessageKey.KICK_FULL_SERVER));
            return true;
        }

        // Server is full and player is VIP; attempt to kick a non-VIP player to make room
        Collection<? extends Player> onlinePlayers = bukkitService.getOnlinePlayers();
        if (onlinePlayers.size() < server.getMaxPlayers()) {
            event.allow();
            return false;
        }
        Player nonVipPlayer = generateKickPlayer(onlinePlayers);
        if (nonVipPlayer != null) {
            nonVipPlayer.kickPlayer(messages.retrieveSingle(player, MessageKey.KICK_FOR_VIP));
            event.allow();
            return false;
        } else {
            ConsoleLogger.info("VIP player " + player.getName() + " tried to join, but the server was full");
            event.setKickMessage(messages.retrieveSingle(player, MessageKey.KICK_FULL_SERVER));
            return true;
        }
    }

    /**
     * Checks that the casing in the username corresponds to the one in the database, if so configured.
     *
     * @param connectingName the player name to verify
     * @param auth the auth object associated with the player
     * @throws FailedVerificationException if the verification fails
     */
    public void checkNameCasing(String connectingName, PlayerAuth auth) throws FailedVerificationException {
        if (auth != null && settings.getProperty(RegistrationSettings.PREVENT_OTHER_CASE)) {
            String realName = auth.getRealName(); // might be null or "Player"

            if (StringUtils.isEmpty(realName) || "Player".equals(realName)) {
                dataSource.updateRealName(connectingName.toLowerCase(), connectingName);
            } else if (!realName.equals(connectingName)) {
                throw new FailedVerificationException(MessageKey.INVALID_NAME_CASE, realName, connectingName);
            }
        }
    }

    /**
     * Checks that the player's country is admitted.
     *
     * @param joiningPlayer   the joining player to verify
     * @param address         the player address
     * @param isAuthAvailable whether or not the user is registered
     * @throws FailedVerificationException if the verification fails
     */
    public void checkPlayerCountry(JoiningPlayer joiningPlayer, String address,
                                   boolean isAuthAvailable) throws FailedVerificationException {
        if ((!isAuthAvailable || settings.getProperty(ProtectionSettings.ENABLE_PROTECTION_REGISTERED))
            && settings.getProperty(ProtectionSettings.ENABLE_PROTECTION)
            && !permissionsManager.hasPermission(joiningPlayer, PlayerStatePermission.BYPASS_COUNTRY_CHECK)
            && !validationService.isCountryAdmitted(address)) {
                throw new FailedVerificationException(MessageKey.COUNTRY_BANNED_ERROR);
        }
    }

    /**
     * Checks if a player with the same name (case-insensitive) is already playing and refuses the
     * connection if so configured.
     *
     * @param name the player name to check
     * @throws FailedVerificationException if the verification fails
     */
    public void checkSingleSession(String name) throws FailedVerificationException {
        if (!settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)) {
            return;
        }

        Player onlinePlayer = bukkitService.getPlayerExact(name);
        if (onlinePlayer != null) {
            throw new FailedVerificationException(MessageKey.USERNAME_ALREADY_ONLINE_ERROR);
        }
    }

    /**
     * Selects a non-VIP player to kick when a VIP player joins the server when full.
     *
     * @param onlinePlayers list of online players
     *
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
}
