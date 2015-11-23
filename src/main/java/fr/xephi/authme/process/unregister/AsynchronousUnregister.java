package fr.xephi.authme.process.unregister;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
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

import java.security.NoSuchAlgorithmException;

/**
 */
public class AsynchronousUnregister {

    protected Player player;
    protected String name;
    protected String password;
    protected boolean force;
    private AuthMe plugin;
    private Messages m = Messages.getInstance();
    private JsonCache playerCache;

    /**
     * Constructor for AsynchronousUnregister.
     *
     * @param player   Player
     * @param password String
     * @param force    boolean
     * @param plugin   AuthMe
     */
    public AsynchronousUnregister(Player player, String password,
                                  boolean force, AuthMe plugin) {
        this.player = player;
        this.password = password;
        this.force = force;
        name = player.getName().toLowerCase();
        this.plugin = plugin;
        this.playerCache = new JsonCache();
    }

    /**
     * Method getIp.
     *
     * @return String
     */
    protected String getIp() {
        return plugin.getIP(player);
    }

    public void process() {
        try {
            if (force || PasswordSecurity.comparePasswordWithHash(password, PlayerCache.getInstance().getAuth(name).getHash(), player.getName())) {
                if (!plugin.database.removeAuth(name)) {
                    player.sendMessage("error");
                    return;
                }
                if (Settings.isForcedRegistrationEnabled) {
                    Utils.teleportToSpawn(player);
                    player.saveData();
                    PlayerCache.getInstance().removePlayer(player.getName().toLowerCase());
                    if (!Settings.getRegisteredGroup.isEmpty())
                        Utils.setGroup(player, GroupType.UNREGISTERED);
                    LimboCache.getInstance().addLimboPlayer(player);
                    int delay = Settings.getRegistrationTimeout * 20;
                    int interval = Settings.getWarnMessageInterval;
                    BukkitScheduler scheduler = plugin.getServer().getScheduler();
                    if (delay != 0) {
                        BukkitTask id = scheduler.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), delay);
                        LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
                    }
                    LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(scheduler.runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.send("reg_msg"), interval)));
                    m.send(player, "unregistered");
                    ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                    return;
                }
                if (!Settings.unRegisteredGroup.isEmpty()) {
                    Utils.setGroup(player, Utils.GroupType.UNREGISTERED);
                }
                PlayerCache.getInstance().removePlayer(player.getName().toLowerCase());
                // check if Player cache File Exist and delete it, preventing
                // duplication of items
                if (playerCache.doesCacheExist(player)) {
                    playerCache.removeCache(player);
                }
                if (Settings.applyBlindEffect)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Settings.getRegistrationTimeout * 20, 2));
                if (!Settings.isMovementAllowed && Settings.isRemoveSpeedEnabled) {
                    player.setWalkSpeed(0.0f);
                    player.setFlySpeed(0.0f);
                }
                m.send(player, "unregistered");
                ConsoleLogger.info(player.getDisplayName() + " unregistered himself");
                Utils.teleportToSpawn(player);
            } else {
                m.send(player, "wrong_pwd");
            }
        } catch (NoSuchAlgorithmException ignored) {
        }
    }
}
