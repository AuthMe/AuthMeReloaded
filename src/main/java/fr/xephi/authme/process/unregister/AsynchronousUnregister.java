package fr.xephi.authme.process.unregister;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;

public class AsynchronousUnregister implements AsynchronousProcess {

    @Inject
    private AuthMe plugin;

    @Inject
    private DataSource dataSource;

    @Inject
    private ProcessService service;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private LimboCache limboCache;

    @Inject
    private BukkitService bukkitService;

    AsynchronousUnregister() { }

    public void unregister(Player player, String password, boolean force) {
        final String name = player.getName().toLowerCase();
        PlayerAuth cachedAuth = playerCache.getAuth(name);
        if (force || passwordSecurity.comparePassword(password, cachedAuth.getPassword(), player.getName())) {
            if (!dataSource.removeAuth(name)) {
                service.send(player, MessageKey.ERROR);
                return;
            }
            int timeOut = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
            if (service.getProperty(RegistrationSettings.FORCE)) {
                Utils.teleportToSpawn(player);
                player.saveData();
                playerCache.removePlayer(player.getName().toLowerCase());
                if (!Settings.getRegisteredGroup.isEmpty()) {
                    service.setGroup(player, AuthGroupType.UNREGISTERED);
                }
                limboCache.addLimboPlayer(player);
                LimboPlayer limboPlayer = limboCache.getLimboPlayer(name);
                int interval = service.getProperty(RegistrationSettings.MESSAGE_INTERVAL);
                if (timeOut != 0) {
                    BukkitTask id = bukkitService.runTaskLater(new TimeoutTask(plugin, name, player), timeOut);
                    limboPlayer.setTimeoutTask(id);
                }
                limboPlayer.setMessageTask(bukkitService.runTask(new MessageTask(bukkitService,
                    plugin.getMessages(), name, MessageKey.REGISTER_MESSAGE, interval)));
                service.send(player, MessageKey.UNREGISTERED_SUCCESS);
                ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                return;
            }
            if (!Settings.unRegisteredGroup.isEmpty()) {
                service.setGroup(player, AuthGroupType.UNREGISTERED);
            }
            playerCache.removePlayer(name);

            // Apply blind effect
            if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeOut, 2));
            }
            service.send(player, MessageKey.UNREGISTERED_SUCCESS);
            ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
        } else {
            service.send(player, MessageKey.WRONG_PASSWORD);
        }
    }
}
