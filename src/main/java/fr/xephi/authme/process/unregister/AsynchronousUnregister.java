package fr.xephi.authme.process.unregister;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Utils.GroupType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

public class AsynchronousUnregister implements Process {

    private final Player player;
    private final String name;
    private final String password;
    private final boolean force;
    private final AuthMe plugin;
    private final JsonCache playerCache;
    private final ProcessService service;

    /**
     * Constructor.
     *
     * @param player The player to perform the action for
     * @param password The password
     * @param force True to bypass password validation
     * @param plugin The plugin instance
     * @param service The process service
     */
    public AsynchronousUnregister(Player player, String password, boolean force, AuthMe plugin,
                                  ProcessService service) {
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.password = password;
        this.force = force;
        this.plugin = plugin;
        this.playerCache = new JsonCache();
        this.service = service;
    }

    @Override
    public void run() {
        PlayerAuth cachedAuth = PlayerCache.getInstance().getAuth(name);
        if (force || plugin.getPasswordSecurity().comparePassword(
            password, cachedAuth.getPassword(), player.getName())) {
            if (!service.getDataSource().removeAuth(name)) {
                service.send(player, MessageKey.ERROR);
                return;
            }
            int timeOut = service.getProperty(RestrictionSettings.TIMEOUT) * 20;
            if (Settings.isForcedRegistrationEnabled) {
                Utils.teleportToSpawn(player);
                player.saveData();
                PlayerCache.getInstance().removePlayer(player.getName().toLowerCase());
                if (!Settings.getRegisteredGroup.isEmpty()) {
                    Utils.setGroup(player, GroupType.UNREGISTERED);
                }
                LimboCache.getInstance().addLimboPlayer(player);
                LimboPlayer limboPlayer = LimboCache.getInstance().getLimboPlayer(name);
                int interval = service.getProperty(RegistrationSettings.MESSAGE_INTERVAL);
                if (timeOut != 0) {
                    BukkitTask id = service.runTaskLater(new TimeoutTask(plugin, name, player), timeOut);
                    limboPlayer.setTimeoutTask(id);
                }
                limboPlayer.setMessageTask(service.runTask(new MessageTask(service.getBukkitService(),
                    plugin.getMessages(), name, MessageKey.REGISTER_MESSAGE, interval)));
                service.send(player, MessageKey.UNREGISTERED_SUCCESS);
                ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                return;
            }
            if (!Settings.unRegisteredGroup.isEmpty()) {
                Utils.setGroup(player, Utils.GroupType.UNREGISTERED);
            }
            PlayerCache.getInstance().removePlayer(name);
            // check if Player cache File Exist and delete it, preventing
            // duplication of items
            if (playerCache.doesCacheExist(player)) {
                playerCache.removeCache(player);
            }
            // Apply blind effect
            if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeOut, 2));
            }
            service.send(player, MessageKey.UNREGISTERED_SUCCESS);
            ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
            Utils.teleportToSpawn(player);
        } else {
            service.send(player, MessageKey.WRONG_PASSWORD);
        }
    }
}
