package fr.xephi.authme.task;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import org.bukkit.entity.Player;

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
        PasswordSecurity passwordSecurity = plugin.getPasswordSecurity();

        final String name = player.getName().toLowerCase();
        PlayerAuth auth = PlayerCache.getInstance().getAuth(name);
        if (passwordSecurity.comparePassword(oldPassword, auth.getPassword(), player.getName())) {
            HashedPassword hashedPassword = passwordSecurity.computeHash(newPassword, name);
            auth.setPassword(hashedPassword);

            if (!plugin.getDataSource().updatePassword(auth)) {
                m.send(player, MessageKey.ERROR);
                return;
            }

            PlayerCache.getInstance().updatePlayer(auth);
            m.send(player, MessageKey.PASSWORD_CHANGED_SUCCESS);
            ConsoleLogger.info(player.getName() + " changed his password");
            if (Settings.bungee) {
                final String hash = hashedPassword.getHash();
                final String salt = hashedPassword.getSalt();
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
    }
}

