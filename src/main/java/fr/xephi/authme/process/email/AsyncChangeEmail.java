package fr.xephi.authme.process.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;

import java.util.Arrays;

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
        try {
            String playerName = player.getName().toLowerCase();

            if (Settings.getmaxRegPerEmail > 0) {
                if (!plugin.getPermissionsManager().hasPermission(player, "authme.allow2accounts")
                    && plugin.database.getAllAuthsByEmail(newEmail).size() >= Settings.getmaxRegPerEmail) {
                    m.send(player, MessageKey.MAX_REGISTER_EXCEEDED);
                    return;
                }
            }

            if (PlayerCache.getInstance().isAuthenticated(playerName)) {
                if (!newEmail.equals(newEmailVerify)) {
                    m.send(player, MessageKey.CONFIRM_EMAIL_MESSAGE);
                    return;
                }
                PlayerAuth auth = PlayerCache.getInstance().getAuth(playerName);
                if (oldEmail != null) {
                    if (auth.getEmail() == null || auth.getEmail().equals("your@email.com") || auth.getEmail().isEmpty()) {
                        m.send(player, MessageKey.USAGE_ADD_EMAIL);
                        return;
                    }
                    if (!oldEmail.equals(auth.getEmail())) {
                        m.send(player, MessageKey.INVALID_OLD_EMAIL);
                        return;
                    }
                }
                if (!Settings.isEmailCorrect(newEmail)) {
                    m.send(player, MessageKey.INVALID_NEW_EMAIL);
                    return;
                }
                String old = auth.getEmail();
                auth.setEmail(newEmail);
                if (!plugin.database.updateEmail(auth)) {
                    m.send(player, MessageKey.ERROR);
                    auth.setEmail(old);
                    return;
                }
                PlayerCache.getInstance().updatePlayer(auth);
                if (oldEmail == null) {
                    m.send(player, MessageKey.EMAIL_ADDED_SUCCESS);
                    player.sendMessage(auth.getEmail());
                    return;
                }
                m.send(player, MessageKey.EMAIL_CHANGED_SUCCESS);
                // TODO ljacqu 20151124: Did I really miss "email_defined" or is it not present in the 'en' messages?
                // player.sendMessage(Arrays.toString(m.send("email_defined")) + auth.getEmail());
            } else {
                if (plugin.database.isAuthAvailable(playerName)) {
                    m.send(player, MessageKey.LOGIN_MESSAGE);
                } else {
                    if (Settings.emailRegistration) {
                        m.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
                    } else {
                        m.send(player, MessageKey.REGISTER_MESSAGE);
                    }
                }
            }
        } catch (Exception e) {
            ConsoleLogger.showError(e.getMessage());
            ConsoleLogger.writeStackTrace(e);
            m.send(player, MessageKey.ERROR);
        }
    }
}
