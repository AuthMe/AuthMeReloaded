package fr.xephi.authme.process.unregister;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.LimboPlayerTaskManager;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

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

    @Inject
    private LimboPlayerTaskManager limboPlayerTaskManager;

    AsynchronousUnregister() { }


    public void unregister(Player player, String password, boolean force) {
        final String name = player.getName().toLowerCase();
        PlayerAuth cachedAuth = playerCache.getAuth(name);
        if (force || passwordSecurity.comparePassword(password, cachedAuth.getPassword(), player.getName())) {
            if (!dataSource.removeAuth(name)) {
                service.send(player, MessageKey.ERROR);
                return;
            }

            if (service.getProperty(RegistrationSettings.FORCE)) {
                Utils.teleportToSpawn(player);
                player.saveData();
                playerCache.removePlayer(player.getName().toLowerCase());
                if (!Settings.getRegisteredGroup.isEmpty()) {
                    service.setGroup(player, AuthGroupType.UNREGISTERED);
                }
                limboCache.addLimboPlayer(player);
                limboPlayerTaskManager.registerTimeoutTask(player);
                limboPlayerTaskManager.registerMessageTask(name, MessageKey.REGISTER_MESSAGE);

                service.send(player, MessageKey.UNREGISTERED_SUCCESS);
                ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                return; // TODO ljacqu 20160612: Why return here? No blind effect? Player not removed from PlayerCache?
            }
            if (!Settings.unRegisteredGroup.isEmpty()) {
                service.setGroup(player, AuthGroupType.UNREGISTERED);
            }
            playerCache.removePlayer(name);

            // Apply blind effect
            int timeout = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
            if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeout, 2));
            }
            service.send(player, MessageKey.UNREGISTERED_SUCCESS);
            ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
        } else {
            service.send(player, MessageKey.WRONG_PASSWORD);
        }
    }
}
