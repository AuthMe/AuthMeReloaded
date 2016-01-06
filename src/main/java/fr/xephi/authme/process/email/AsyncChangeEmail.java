package fr.xephi.authme.process.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PlayerPermission;
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
    private final String newEmailVerify;
    private final Messages m;

    public AsyncChangeEmail(Player player, AuthMe plugin, String oldEmail, String newEmail, String newEmailVerify) {
        this.m = plugin.getMessages();
        this.player = player;
        this.plugin = plugin;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
        this.newEmailVerify = newEmailVerify;
    }

    public AsyncChangeEmail(Player player, AuthMe plugin, String oldEmail, String newEmail) {
        this(player, plugin, oldEmail, newEmail, newEmail);
    }

    public void process() {
        String playerName = player.getName().toLowerCase();
        if (PlayerCache.getInstance().isAuthenticated(playerName)) {
            if (!newEmail.equals(newEmailVerify)) {
                m.send(player, MessageKey.CONFIRM_EMAIL_MESSAGE);
                return;
            }
            PlayerAuth auth = PlayerCache.getInstance().getAuth(playerName);
            String currentEmail = auth.getEmail();
            if (oldEmail != null) {
                if (StringUtils.isEmpty(currentEmail) || currentEmail.equals("your@email.com")) {
                    m.send(player, MessageKey.USAGE_ADD_EMAIL);
                    return;
                }
                if (!oldEmail.equals(currentEmail)) {
                    m.send(player, MessageKey.INVALID_OLD_EMAIL);
                    return;
                }
            }
            if (!Settings.isEmailCorrect(newEmail)) {
                m.send(player, MessageKey.INVALID_NEW_EMAIL);
                return;
            }
            auth.setEmail(newEmail);
            if (!plugin.getDataSource().updateEmail(auth)) {
                m.send(player, MessageKey.ERROR);
                auth.setEmail(currentEmail);
                return;
            }
            PlayerCache.getInstance().updatePlayer(auth);
            if (oldEmail == null) {
                m.send(player, MessageKey.EMAIL_ADDED_SUCCESS);
                player.sendMessage(auth.getEmail());
                return;
            }
            m.send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
        } else {
            if (plugin.getDataSource().isAuthAvailable(playerName)) {
                m.send(player, MessageKey.LOGIN_MESSAGE);
            } else {
                if (Settings.emailRegistration) {
                    m.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
                } else {
                    m.send(player, MessageKey.REGISTER_MESSAGE);
                }
            }
        }

    }
}
