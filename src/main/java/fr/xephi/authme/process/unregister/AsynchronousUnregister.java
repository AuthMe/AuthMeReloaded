package fr.xephi.authme.process.unregister;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Utils.GroupType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class AsynchronousUnregister {

    private final Player player;
    private final String name;
    private final String password;
    private final boolean force;
    private final AuthMe plugin;
    private final Messages m;
    private final JsonCache playerCache;

    /**
     * Constructor for AsynchronousUnregister.
     *
     * @param player   Player
     * @param password String
     * @param force    boolean
     * @param plugin   AuthMe
     */
    public AsynchronousUnregister(Player player, String password, boolean force, AuthMe plugin) {
        this.m = plugin.getMessages();
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.password = password;
        this.force = force;
        this.plugin = plugin;
        this.playerCache = new JsonCache();
    }

    protected String getIp() {
        return plugin.getIP(player);
    }

    public void process() {
        if (force || plugin.getPasswordSecurity().comparePassword(password, PlayerCache.getInstance().getAuth(name).getHash(), player.getName())) {
            if (!plugin.getDataSource().removeAuth(name)) {
                m.send(player, MessageKey.ERROR);
                return;
            }
            int timeOut = Settings.getRegistrationTimeout * 20;
            if (Settings.isForcedRegistrationEnabled) {
                Utils.teleportToSpawn(player);
                player.saveData();
                PlayerCache.getInstance().removePlayer(player.getName().toLowerCase());
                if (!Settings.getRegisteredGroup.isEmpty()) {
                    Utils.setGroup(player, GroupType.UNREGISTERED);
                }
                LimboCache.getInstance().addLimboPlayer(player);
                LimboPlayer limboPlayer = LimboCache.getInstance().getLimboPlayer(name);
                int interval = Settings.getWarnMessageInterval;
                BukkitScheduler scheduler = plugin.getServer().getScheduler();
                if (timeOut != 0) {
                    BukkitTask id = scheduler.runTaskLaterAsynchronously(plugin,
                        new TimeoutTask(plugin, name, player), timeOut);
                    limboPlayer.setTimeoutTaskId(id);
                }
                limboPlayer.setMessageTaskId(scheduler.runTaskAsynchronously(plugin,
                        new MessageTask(plugin, name, m.retrieve(MessageKey.REGISTER_MESSAGE), interval)));
                m.send(player, MessageKey.UNREGISTERED_SUCCESS);
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
            if (Settings.applyBlindEffect) {
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeOut, 2));
            }
            m.send(player, MessageKey.UNREGISTERED_SUCCESS);
            ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
            Utils.teleportToSpawn(player);
        } else {
            m.send(player, MessageKey.WRONG_PASSWORD);
        }
    }
}
