package fr.xephi.authme.process.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;

/**
 * Async task to add an email to an account.
 */
public class AsyncAddEmail {

    private final Player player;
    private final String email;
    private final Messages messages;
    private final DataSource dataSource;
    private final PlayerCache playerCache;
    private final NewSetting settings;

    public AsyncAddEmail(Player player, AuthMe plugin, String email, DataSource dataSource,
                         PlayerCache playerCache, NewSetting settings) {
        this.messages = plugin.getMessages();
        this.player = player;
        this.email = email;
        this.dataSource = dataSource;
        this.playerCache = playerCache;
        this.settings = settings;
    }

    public void process() {
        String playerName = player.getName().toLowerCase();

        if (playerCache.isAuthenticated(playerName)) {
            PlayerAuth auth = playerCache.getAuth(playerName);
            final String currentEmail = auth.getEmail();

            if (currentEmail != null && !"your@email.com".equals(currentEmail)) {
                messages.send(player, MessageKey.USAGE_CHANGE_EMAIL);
            } else if (!Utils.isEmailCorrect(email, settings)) {
                messages.send(player, MessageKey.INVALID_EMAIL);
            } else if (dataSource.isEmailStored(email)) {
                messages.send(player, MessageKey.EMAIL_ALREADY_USED_ERROR);
            } else if (dataSource.updateEmail(auth)) {
                auth.setEmail(email);
                playerCache.updatePlayer(auth);
                messages.send(player, MessageKey.EMAIL_ADDED_SUCCESS);
            } else {
                messages.send(player, MessageKey.ERROR);
            }
        } else {
            sendUnloggedMessage(dataSource);
        }
    }

    private void sendUnloggedMessage(DataSource dataSource) {
        if (dataSource.isAuthAvailable(player.getName())) {
            messages.send(player, MessageKey.LOGIN_MESSAGE);
        } else if (Settings.emailRegistration) {
            messages.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        } else {
            messages.send(player, MessageKey.REGISTER_MESSAGE);
        }
    }

}
