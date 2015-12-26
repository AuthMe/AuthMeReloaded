package fr.xephi.authme.task;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;

import java.security.NoSuchAlgorithmException;

public class ChangePasswordTask implements Runnable {

    private final AuthMe plugin;
    private final Player player;
    private final String oldPassword;
    private final String newPassword;

    public ChangePasswordTask(AuthMe plugin, Player player, String oldPassword, String newPassword) {
        this.plugin = plugin;
        this.player = player;
        this.oldPassword = oldPassword;
        this.newPassword = newPassword;
    }

    @Override
    public void run() {
        Messages m = plugin.getMessages();
        try {
            final String name = player.getName().toLowerCase();
            String hashNew = PasswordSecurity.getHash(Settings.getPasswordHash, newPassword, name);
            PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
            if (PasswordSecurity.comparePasswordWithHash(oldPassword, auth.getHash(), player.getName())) {
                auth.setHash(hashNew);
                if (PasswordSecurity.userSalt.containsKey(name) && PasswordSecurity.userSalt.get(name) != null) {
                    auth.setSalt(PasswordSecurity.userSalt.get(name));
                } else {
                    auth.setSalt("");
                }
                if (!plugin.database.updatePassword(auth)) {
                    m.send(player, MessageKey.ERROR);
                    return;
                }
                plugin.database.updateSalt(auth);
                PlayerCache.getInstance().updatePlayer(auth);
                m.send(player, MessageKey.PASSWORD_CHANGED_SUCCESS);
                ConsoleLogger.info(player.getName() + " changed his password");
                if (Settings.bungee)
                {
                	final String hash = auth.getHash();
                	final String salt = auth.getSalt();
                    plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable(){

    					@Override
    					public void run() {
    					        ByteArrayDataOutput out = ByteStreams.newDataOutput();
    					        out.writeUTF("Forward");
    					        out.writeUTF("ALL");
    					        out.writeUTF("AuthMe");
    					        out.writeUTF("changepassword;" + name + ";" + hash + ";" + salt);
    					        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    					}
                    });
                }
            } else {
                m.send(player, MessageKey.WRONG_PASSWORD);
            }
        } catch (NoSuchAlgorithmException ex) {
            ConsoleLogger.showError(ex.getMessage());
            m.send(player, MessageKey.ERROR);
        }
    }
}

