package fr.xephi.authme.process.email;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
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

    /**
     * Constructor for AsyncChangeEmail.
     *
     * @param player         Player
     * @param plugin         AuthMe
     * @param oldEmail       String
     * @param newEmail       String
     * @param newEmailVerify String
     */
    public AsyncChangeEmail(Player player, AuthMe plugin, String oldEmail, String newEmail, String newEmailVerify) {
        this.player = player;
        this.plugin = plugin;
        this.oldEmail = oldEmail;
        this.newEmail = newEmail;
        this.newEmailVerify = newEmailVerify;
        this.m = Messages.getInstance();
    }

    /**
     * Constructor for AsyncChangeEmail.
     *
     * @param player   Player
     * @param plugin   AuthMe
     * @param oldEmail String
     * @param newEmail String
     */
    public AsyncChangeEmail(Player player, AuthMe plugin, String oldEmail, String newEmail) {
        this(player, plugin, oldEmail, newEmail, newEmail);
    }

    public void process() {
        try {
            String playerName = player.getName().toLowerCase();

            if (Settings.getmaxRegPerEmail > 0) {
                if (!plugin.getPermissionsManager().hasPermission(player, "authme.allow2accounts") && plugin.database.getAllAuthsByEmail(newEmail).size() >= Settings.getmaxRegPerEmail) {
                    m.send(player, "max_reg");
                    return;
                }
            }

            if (PlayerCache.getInstance().isAuthenticated(playerName)) {
                if (!newEmail.equals(newEmailVerify)) {
                    m.send(player, "email_confirm");
                    return;
                }
                PlayerAuth auth = PlayerCache.getInstance().getAuth(playerName);
                if (oldEmail != null) {
                    if (auth.getEmail() == null || auth.getEmail().equals("your@email.com") || auth.getEmail().isEmpty()) {
                        m.send(player, "usage_email_add");
                        return;
                    }
                    if (!oldEmail.equals(auth.getEmail())) {
                        m.send(player, "old_email_invalid");
                        return;
                    }
                }
                if (!Settings.isEmailCorrect(newEmail)) {
                    m.send(player, "new_email_invalid");
                    return;
                }
                String old = auth.getEmail();
                auth.setEmail(newEmail);
                if (!plugin.database.updateEmail(auth)) {
                    m.send(player, "error");
                    auth.setEmail(old);
                    return;
                }
                PlayerCache.getInstance().updatePlayer(auth);
                if (oldEmail == null) {
                    m.send(player, "email_added");
                    player.sendMessage(auth.getEmail());
                    return;
                }
                m.send(player, "email_changed");
                player.sendMessage(Arrays.toString(m.send("email_defined")) + auth.getEmail());
            } else {
                if (plugin.database.isAuthAvailable(playerName)) {
                    m.send(player, "login_msg");
                } else {
                    if (Settings.emailRegistration)
                        m.send(player, "reg_email_msg");
                    else
                        m.send(player, "reg_msg");
                }
            }
        } catch (Exception e) {
            ConsoleLogger.showError(e.getMessage());
            ConsoleLogger.writeStackTrace(e);
            m.send(player, "error");
        }
    }
}
