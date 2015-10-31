package fr.xephi.authme.process.unregister;

import java.security.NoSuchAlgorithmException;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.Utils.GroupType;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class AsyncronousUnregister {

    protected Player player;
    protected String name;
    private AuthMe plugin;
    private Messages m = Messages.getInstance();
	protected String password;
	protected boolean force;
	private JsonCache playerCache;

    public AsyncronousUnregister(Player player, String password,
            boolean force, AuthMe plugin) {
        this.player = player;
        this.password = password;
        this.force = force;
        name = player.getName().toLowerCase();
        this.plugin = plugin;
        this.playerCache = new JsonCache();
    }

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
                    if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
                        Location spawn = plugin.getSpawnLocation(player);
                        SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawn, false);
                        plugin.getServer().getPluginManager().callEvent(tpEvent);
                        if (!tpEvent.isCancelled()) {
                            player.teleport(tpEvent.getTo());
                        }
                    }

                    player.saveData();
                    PlayerCache.getInstance().removePlayer(player.getName().toLowerCase());
                    if (!Settings.getRegisteredGroup.isEmpty())
                        Utils.setGroup(player, GroupType.UNREGISTERED);
                    LimboCache.getInstance().addLimboPlayer(player);
                    int delay = Settings.getRegistrationTimeout * 20;
                    int interval = Settings.getWarnMessageInterval;
                    BukkitScheduler sched = plugin.getServer().getScheduler();
                    if (delay != 0) {
                        BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), delay);
                        LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
                    }
                    LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.send("reg_msg"), interval)));
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
                if (Settings.isTeleportToSpawnEnabled && !Settings.noTeleport) {
                    Location spawn = plugin.getSpawnLocation(player);
                    SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawn, false);
                    plugin.getServer().getPluginManager().callEvent(tpEvent);
                    if (!tpEvent.isCancelled()) {
                        if (!tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).isLoaded()) {
                            tpEvent.getTo().getWorld().getChunkAt(tpEvent.getTo()).load();
                        }
                        player.teleport(tpEvent.getTo());
                    }
                }
                return;
            } else {
                m.send(player, "wrong_pwd");
            }
        } catch (NoSuchAlgorithmException ex) {
        }
    }
}
