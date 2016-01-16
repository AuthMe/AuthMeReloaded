package fr.xephi.authme.process.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.entity.Player;

/**
 * Async task for changing the email.
 */
public class AsyncChangeEmail {

    private final Player player;
    private final String oldEmail;
    private final String newEmail;
    private final Messages m;
    private final PlayerCache playerCache;
    private final DataSource dataSource;

    public AsyncChangeEmail(Player player, AuthMe plugin, String oldEmail,
                            String newEmail, DataSource dataSource, PlayerCache playerCache) {
        this.m = plugin.getMessages();
        this.player = player;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
        this.playerCache = playerCache;
        this.dataSource = dataSource;
    }

    public void process() {
        String playerName = player.getName().toLowerCase();
        if (playerCache.isAuthenticated(playerName)) {
            PlayerAuth auth = playerCache.getAuth(playerName);
            String currentEmail = auth.getEmail();

            if (currentEmail == null) {
                m.send(player, MessageKey.USAGE_ADD_EMAIL);
            } else if (isEmailInvalid(newEmail)) {
                m.send(player, MessageKey.INVALID_NEW_EMAIL);
            } else if (!oldEmail.equals(currentEmail)) {
                m.send(player, MessageKey.INVALID_OLD_EMAIL);
            } else {
                saveNewEmail(auth);
            }
        } else {
            outputUnloggedMessage();
        }
    }

    private static boolean isEmailInvalid(String email) {
        return StringUtils.isEmpty(email) || "your@email.com".equals(email)
            || !Settings.isEmailCorrect(email);
    }

    private void saveNewEmail(PlayerAuth auth) {
        auth.setEmail(newEmail);
        if (dataSource.updateEmail(auth)) {
            playerCache.updatePlayer(auth);
            m.send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
        } else {
            m.send(player, MessageKey.ERROR);
            auth.setEmail(newEmail);
        }
    }

    private void outputUnloggedMessage() {
        if (dataSource.isAuthAvailable(player.getName())) {
            m.send(player, MessageKey.LOGIN_MESSAGE);
        } else if (Settings.emailRegistration) {
            m.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        } else {
            m.send(player, MessageKey.REGISTER_MESSAGE);
        }
    }
}
