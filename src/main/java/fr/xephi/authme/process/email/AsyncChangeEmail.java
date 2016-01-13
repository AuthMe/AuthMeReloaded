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
 */
public class AsyncChangeEmail {

    private final Player player;
    private final AuthMe plugin;
    private final String oldEmail;
    private final String newEmail;
    private final Messages m;

    public AsyncChangeEmail(Player player, AuthMe plugin, String oldEmail, String newEmail) {
        this.m = plugin.getMessages();
        this.player = player;
        this.plugin = plugin;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
    }

    public void process() {
        String playerName = player.getName().toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(playerName)) {
            PlayerAuth auth = PlayerCache.getInstance().getAuth(playerName);
            String currentEmail = auth.getEmail();

            if (currentEmail == null) {
                m.send(player, MessageKey.USAGE_ADD_EMAIL);
            } else if (StringUtils.isEmpty(newEmail) || "your@email.com".equals(newEmail)) {
                m.send(player, MessageKey.INVALID_EMAIL);
            } else if (!oldEmail.equals(currentEmail)) {
                m.send(player, MessageKey.INVALID_OLD_EMAIL);
            } else if (Settings.isEmailCorrect(newEmail)) {
                m.send(player, MessageKey.INVALID_NEW_EMAIL);
            } else {
                saveNewEmail(auth);
            }
        } else {
            outputUnloggedMessage();
        }
    }

    private void saveNewEmail(PlayerAuth auth) {
        auth.setEmail(newEmail);
        if (plugin.getDataSource().updateEmail(auth)) {
            PlayerCache.getInstance().updatePlayer(auth);
            m.send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
        } else {
            m.send(player, MessageKey.ERROR);
            auth.setEmail(newEmail);
        }
    }

    private void outputUnloggedMessage() {
        if (plugin.getDataSource().isAuthAvailable(player.getName())) {
            m.send(player, MessageKey.LOGIN_MESSAGE);
        } else if (Settings.emailRegistration) {
            m.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
        } else {
            m.send(player, MessageKey.REGISTER_MESSAGE);
        }
    }
}
