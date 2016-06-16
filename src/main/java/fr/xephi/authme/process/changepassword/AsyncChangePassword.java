package fr.xephi.authme.process.changepassword;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;

import javax.inject.Inject;

public class AsyncChangePassword implements AsynchronousProcess {

    @Inject
    private AuthMe plugin;

    @Inject
    private DataSource dataSource;

    @Inject
    private ProcessService processService;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private BukkitService bukkitService;

    AsyncChangePassword() { }


    public void changePassword(final Player player, String oldPassword, String newPassword) {
        final String name = player.getName().toLowerCase();
        PlayerAuth auth = playerCache.getAuth(name);
        if (passwordSecurity.comparePassword(oldPassword, auth.getPassword(), player.getName())) {
            HashedPassword hashedPassword = passwordSecurity.computeHash(newPassword, name);
            auth.setPassword(hashedPassword);

            if (!dataSource.updatePassword(auth)) {
                processService.send(player, MessageKey.ERROR);
                return;
            }

            playerCache.updatePlayer(auth);
            processService.send(player, MessageKey.PASSWORD_CHANGED_SUCCESS);
            ConsoleLogger.info(player.getName() + " changed his password");
            if (processService.getProperty(HooksSettings.BUNGEECORD)) {
                final String hash = hashedPassword.getHash();
                final String salt = hashedPassword.getSalt();
                bukkitService.scheduleSyncDelayedTask(new Runnable() {
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
            processService.send(player, MessageKey.WRONG_PASSWORD);
        }
    }
}

